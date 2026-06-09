package com.example.arplitka.iosapp.platform.render

import com.example.arplitka.iosapp.platform.ar.ArWorldScnMatrix
import com.example.arplitka.iosapp.platform.ar.CenterPlaneHit
import com.example.arplitka.iosapp.platform.ar.confirmedWorldFloorPoint
import com.example.arplitka.iosapp.platform.ar.confirmedWorldScnMatrix
import com.example.arplitka.iosapp.platform.ar.previewWorldFloorPoint
import com.example.arplitka.iosapp.platform.ar.previewWorldScnMatrix
import com.example.arplitka.iosapp.bridge.PG_PLANE_CLASSIFICATION_FLOOR
import com.example.arplitka.iosapp.bridge.PG_PLANE_CLASSIFICATION_TABLE
import com.example.arplitka.iosapp.bridge.PG_PLANE_CLASSIFICATION_SEAT
import com.example.arplitka.iosapp.bridge.pg_create_grid_line_geometry
import com.example.arplitka.iosapp.bridge.pg_create_polygon_grid_line_geometry_from_vertices
import com.example.arplitka.iosapp.bridge.pg_create_world_aligned_placement_patch_grid_geometry
import com.example.arplitka.iosapp.bridge.pg_geometry_signature
import com.example.arplitka.iosapp.bridge.pg_plane_classification
import com.example.arplitka.iosapp.bridge.pg_plane_is_floor_like
import com.example.arplitka.iosapp.bridge.pg_polygon_area
import com.example.arplitka.shared.ar.contracts.model.ArPoint3D
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.ARKit.ARPlaneAnchor
import platform.CoreFoundation.CFAbsoluteTimeGetCurrent
import platform.Foundation.NSUUID
import platform.SceneKit.SCNGeometry
import platform.SceneKit.SCNMaterial
import platform.SceneKit.SCNNode
import platform.SceneKit.SCNVector3Make
import platform.UIKit.UIColor
import kotlin.math.abs
import kotlin.math.sqrt

internal const val MAX_OVERLAY_DISTANCE_M = 10f
internal const val ELEVATED_SUPPRESS_DELTA_M = 0.15f
internal const val STICKY_FLOOR_Y_RESET_M = 1.0f

private const val OVERLAY_NODE_NAME = "plane-surface-overlay"
private const val RETICLE_PATCH_NODE_NAME = "plane-surface-reticle-patch"
private const val PLACEMENT_PATCH_NODE_NAME = "plane-placement-reticle-patch"
private const val GRID_CELL_METERS = 0.6f
private const val GRID_LINE_WIDTH_M = 0.015f
/** Thinner than grid — readable zone edge without the old thick "wall box". */
private const val GRID_BOUNDARY_LINE_WIDTH_M = 0.006f
private const val RETICLE_SURFACE_PATCH_M = 1.5f
private const val PLACEMENT_SURFACE_PATCH_M = 0.55f
private const val PLACEMENT_PATCH_CELL_M = 0.18f
private const val PLACEMENT_PATCH_LINE_WIDTH_M = 0.01f
private const val PLACEMENT_PATCH_BOUNDARY_WIDTH_M = 0.012f
/** Same threshold as Android / placement — overlays below this are hidden. */
private const val MIN_RENDER_AREA_M2 = MIN_FLOOR_AREA_M2
private const val MAX_SURFACE_OVERLAYS = 1
/** Planes closer than this in world Y are treated as one level — keep only the largest overlay. */
private const val SAME_LEVEL_Y_DELTA_M = 0.10f
private const val ELEVATED_Y_DELTA_M = SAME_LEVEL_Y_DELTA_M
private const val MAX_RETICLE_ABOVE_FLOOR_M = 2.2f
private const val MAX_RETICLE_BELOW_FLOOR_M = 0.18f
private const val MAX_RETICLE_HIT_DISTANCE_M = 5.0
private const val MAX_PLANE_ABOVE_CAMERA_M = 0.25f
private const val MAX_PLANE_BELOW_CAMERA_M = 3.5f
private const val MAX_RETICLE_ABOVE_CAMERA_M = 0.12f
private const val FLOOR_SWITCH_AREA_RATIO = 1.18f
private const val ANCHOR_GEOMETRY_MIN_INTERVAL_S = 0.12
private const val GRID_ALPHA_NORMAL = 0.92
private const val GRID_ALPHA_DEGRADED = 0.35
private const val OVERLAY_TRANSFORM_DEBOUNCE_FRAMES = 3

internal data class IosArScanSurfaceContext(
    val allowEstimatedPatch: Boolean,
    /** B.3: weak tracking — hide extent-only overlays, no estimated scan content. */
    val strictConfirmedOverlaysOnly: Boolean,
    val cameraWorldYMeters: Float?,
    val cameraWorldPosition: ArPoint3D? = null
)

internal class OverlayCullStats {
    var distanceCulled: Int = 0
    var elevatedCulled: Int = 0

    fun debugLabel(): String = "d:$distanceCulled/e:$elevatedCulled"
}

internal data class ScanSurfaceDebugStats(
    val largestPlaneAreaM2: Float = 0f,
    val cullStats: OverlayCullStats = OverlayCullStats()
)

internal enum class GridSurfaceMode {
    HIDDEN,
    MULTI_SURFACE,
    MULTI_WITH_RETICLE,
    RETICLE_ONLY
}

/**
 * Scan viz: polygon-following grid per plane (B.2) + optional reticle patch when floor is not visible.
 * Heavy work runs on anchor node events; [applyOverlayBudget] only toggles visibility from cache.
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosArPlaneSurfaceRenderer {
    private val overlays = mutableMapOf<NSUUID, SurfaceOverlay>()
    private val elevationLock = PlaneDotElevationLock()
    private var reticlePatchNode: SCNNode? = null
    private var placementPatchNode: SCNNode? = null
    private var reticleMaterial: SCNMaterial? = null
    private var placementGridMaterial: SCNMaterial? = null
    private var activeMode: GridSurfaceMode = GridSurfaceMode.HIDDEN
    private val areas = mutableMapOf<NSUUID, Float>()
    private var stickyFloorAnchorId: NSUUID? = null
    private var cachedFloorWorldY: Float? = null
    private var contourModeActive = false
    private var placementScanFrozen = false
    private var placementHitOnlyActive = false
    private var overlaysPinnedToWorld = false
    private var sectionFloorLocked = false
    /** After lock: overlay keeps anchor XZ but never copies plane rotation (prevents post-freeze spin). */
    private var overlayRotationFrozen = false
    private var overlayTransformSkipFrames = 0
    private var referenceOverlayDegraded = false
    private var lastDebugStats = ScanSurfaceDebugStats()
    private val asyncGridBuilder = AsyncPolygonGridGeometryBuilder()

    fun prepare() {
        if (reticleMaterial != null) return
        reticleMaterial = createGridLineMaterial()
        reticlePatchNode = SCNNode().apply {
            name = RETICLE_PATCH_NODE_NAME
            geometry = buildExtentGridLineGeometry(
                widthM = RETICLE_SURFACE_PATCH_M,
                depthM = RETICLE_SURFACE_PATCH_M,
                centerX = 0f,
                centerZ = 0f,
                material = reticleMaterial!!
            )
            position = SCNVector3Make(0f, 0f, 0f)
            hidden = true
        }
        placementGridMaterial = createGridLineMaterial()
        placementPatchNode = SCNNode().apply {
            name = PLACEMENT_PATCH_NODE_NAME
            hidden = true
        }
    }

    fun reset() {
        asyncGridBuilder.cancelAll()
        overlays.values.forEach { it.node.removeFromParentNode() }
        overlays.clear()
        elevationLock.clearAll()
        reticlePatchNode?.removeFromParentNode()
        reticlePatchNode = null
        placementPatchNode?.removeFromParentNode()
        placementPatchNode = null
        reticleMaterial = null
        placementGridMaterial = null
        activeMode = GridSurfaceMode.HIDDEN
        areas.clear()
        stickyFloorAnchorId = null
        cachedFloorWorldY = null
        contourModeActive = false
        placementScanFrozen = false
        placementHitOnlyActive = false
        overlaysPinnedToWorld = false
        sectionFloorLocked = false
        overlayRotationFrozen = false
        overlayTransformSkipFrames = 0
        referenceOverlayDegraded = false
        lastDebugStats = ScanSurfaceDebugStats()
    }

    fun scanDebugStats(): ScanSurfaceDebugStats = lastDebugStats

    fun pinCurrentOverlaysToWorldFloor(sceneRoot: SCNNode?) {
        tryPinVisibleOverlays(sceneRoot)
    }

    fun enterPlacementHitOnlyMode() {
        if (placementHitOnlyActive) return
        placementHitOnlyActive = true
        prepare()
        asyncGridBuilder.cancelAll()
        overlays.values.forEach { it.node.hidden = true }
        reticlePatchNode?.hidden = true
        placementPatchNode?.hidden = true
        activeMode = GridSurfaceMode.HIDDEN
    }

    fun exitPlacementHitOnlyMode() {
        if (!placementHitOnlyActive) return
        placementHitOnlyActive = false
        placementPatchNode?.hidden = true
    }

    /**
     * After first point the scan grid becomes obsolete: the contour owns the working floor.
     * Keep only the small placement patch so ARKit plane merges/refines cannot move the UX.
     */
    fun enterPlacementScanFreeze(@Suppress("UNUSED_PARAMETER") sceneRoot: SCNNode?) {
        if (placementScanFrozen) {
            reticlePatchNode?.hidden = true
            overlays.values.forEach { it.node.hidden = true }
            updateActiveMode()
            return
        }
        placementScanFrozen = true
        placementHitOnlyActive = false
        reticlePatchNode?.hidden = true
        placementPatchNode?.hidden = true
        asyncGridBuilder.cancelAll()
        overlays.values.forEach { it.node.hidden = true }
        updateActiveMode()
    }

    fun exitPlacementScanFreeze() {
        if (!placementScanFrozen && !overlaysPinnedToWorld && !sectionFloorLocked) return
        placementScanFrozen = false
        placementHitOnlyActive = false
        sectionFloorLocked = false
        overlayRotationFrozen = false
        if (overlaysPinnedToWorld) {
            overlays.values.forEach { overlay ->
                overlay.node.removeFromParentNode()
                overlay.node.hidden = true
            }
            overlaysPinnedToWorld = false
        }
    }

    fun isPlacementScanFrozen(): Boolean = placementScanFrozen

    /** Called when contour is finalized — hide scan overlays, keep contour geometry on screen. */
    fun enterContourMode() {
        asyncGridBuilder.cancelAll()
        contourModeActive = true
        overlays.values.forEach { it.node.hidden = true }
        reticlePatchNode?.hidden = true
        placementPatchNode?.hidden = true
        activeMode = GridSurfaceMode.HIDDEN
    }

    fun exitContourMode() {
        contourModeActive = false
    }

    fun cacheArea(anchor: ARPlaneAnchor) {
        if (!anchor.isHorizontalTracking()) {
            areas.remove(anchor.identifier)
            return
        }
        areas[anchor.identifier] = pg_polygon_area(anchor.bridgePointer())
    }

    fun hideAll() {
        activeMode = GridSurfaceMode.HIDDEN
        overlays.values.forEach { it.node.hidden = true }
        reticlePatchNode?.hidden = true
        placementPatchNode?.hidden = true
    }

    /**
     * Call from renderer didAdd/didUpdate — draws this plane immediately, no waiting for global winner.
     */
    fun syncPlaneOverlayOnNodeEvent(
        anchor: ARPlaneAnchor,
        anchorNode: SCNNode,
        sceneRoot: SCNNode? = null,
        forceGeometry: Boolean = false
    ): Boolean {
        if (contourModeActive) return false
        if (!anchor.isHorizontalTracking()) return false
        prepare()
        cacheArea(anchor)
        return updateOverlayGeometry(anchor, anchorNode, sceneRoot, force = forceGeometry)
    }

    /**
     * Lightweight per-frame pass: show top-N cached areas, drop removed anchors. No geometry reads here.
     */
    fun applyOverlayBudget(
        anchors: List<ARPlaneAnchor>,
        scanContext: IosArScanSurfaceContext,
        reticleAnchorId: NSUUID? = null
    ) {
        if (contourModeActive) return
        prepare()

        val activeIds = anchors
            .asSequence()
            .filter { it.isHorizontalTracking() }
            .map { it.identifier }
            .toSet()

        val orphanIds = overlays.keys - activeIds
        if (!sectionFloorLocked) {
            orphanIds.forEach { removeOverlay(it) }
        }

        val ranked = anchors
            .asSequence()
            .filter { it.isHorizontalTracking() }
            .mapNotNull { anchor ->
                val area = areas[anchor.identifier] ?: return@mapNotNull null
                if (area < MIN_RENDER_AREA_M2) return@mapNotNull null
                anchor to area
            }
            .sortedByDescending { it.second }
            .map { it.first }
            .toList()

        updateStickyFloor(ranked)
        val floorArea = stickyFloorAnchorId?.let { areas[it] }
        val cullStats = OverlayCullStats()
        val visible = pickVisibleAnchors(
            ranked = ranked,
            scanContext = scanContext,
            floorArea = floorArea,
            cullStats = cullStats,
            reticleAnchorId = reticleAnchorId
        ).map { it.identifier }.toSet()

        lastDebugStats = ScanSurfaceDebugStats(
            largestPlaneAreaM2 = ranked.firstOrNull()?.let { areas[it.identifier] } ?: 0f,
            cullStats = cullStats
        )

        val visibleIds = if (scanContext.strictConfirmedOverlaysOnly) {
            visible.filter { anchorId ->
                ranked.firstOrNull { it.identifier == anchorId }?.hasPolygonBoundary() == true
            }.toSet()
        } else {
            visible
        }

        overlays.forEach { (id, overlay) ->
            overlay.node.hidden = id !in visibleIds
        }

        updateActiveMode()
    }

    /**
     * Pin overlays to the lowest raycast floor height per anchor — ARKit often lifts plane anchors while refining.
     */
    fun applyOverlayElevation(
        anchors: List<ARPlaneAnchor>,
        centerHit: CenterPlaneHit
    ) {
        if (contourModeActive) return
        if (sectionFloorLocked) return
        val hitAnchorId = centerHit.anchor?.identifier
        val raycastY = centerHit.confirmedWorldFloorPoint()?.yMeters
            ?: centerHit.previewWorldFloorPoint()?.yMeters
        val anchorsById = anchors.associateBy { it.identifier }

        overlays.forEach { (id, overlay) ->
            if (overlay.node.hidden) return@forEach
            val anchor = anchorsById[id] ?: return@forEach
            val raycastForAnchor = if (id == hitAnchorId) raycastY else null
            val localY = elevationLock.lockedLocalYOffsetM(anchor, raycastForAnchor)
            overlay.node.position = SCNVector3Make(0f, localY, 0f)
        }
    }

    fun syncReticlePatch(
        rootNode: SCNNode,
        centerHit: CenterPlaneHit,
        floorWorldY: Float?,
        scanContext: IosArScanSurfaceContext,
        largestPlaneAreaM2: Float = 0f
    ): Boolean {
        if (contourModeActive) return false
        prepare()

        if (shouldSuppressReticlePatch(centerHit, floorWorldY, largestPlaneAreaM2)) {
            hideReticlePatch()
            return false
        }

        val hitTransform = pickReticleHit(centerHit, floorWorldY, scanContext, largestPlaneAreaM2) ?: run {
            hideReticlePatch()
            updateActiveMode()
            return false
        }

        val node = reticlePatchNode ?: return false
        pruneDuplicateNamedNodes(rootNode, RETICLE_PATCH_NODE_NAME, keep = node)
        if (node.parentNode != rootNode) {
            node.removeFromParentNode()
            rootNode.addChildNode(node)
        }
        node.transform = hitTransform
        node.hidden = false
        updateActiveMode()
        return true
    }

    fun hideReticlePatch() {
        reticlePatchNode?.hidden = true
        updateActiveMode()
    }

    /**
     * Placement-only: a small working-floor candidate under reticle.
     * The first scan surface is only a reference; no second polygon surface is built here.
     */
    fun syncPlacementExplorationPatch(
        rootNode: SCNNode,
        centerHit: CenterPlaneHit,
        sectionFloorY: Float?,
        show: Boolean
    ): Boolean {
        if (contourModeActive || !placementScanFrozen) return false
        prepare()
        if (!show) {
            placementPatchNode?.hidden = true
            updateActiveMode()
            return false
        }
        val hitPoint = centerHit.placementFloorPoint(sectionFloorY) ?: run {
            placementPatchNode?.hidden = true
            updateActiveMode()
            return false
        }
        val node = placementPatchNode ?: return false
        pruneDuplicateNamedNodes(rootNode, PLACEMENT_PATCH_NODE_NAME, keep = node)
        if (node.parentNode != rootNode) {
            node.removeFromParentNode()
            rootNode.addChildNode(node)
        }
        applyPlacementPatchTransform(node, hitPoint, sectionFloorY)
        node.eulerAngles = SCNVector3Make(0f, 0f, 0f)
        node.hidden = false
        updateActiveMode()
        return true
    }

    fun isPlacementExplorationPatchVisible(): Boolean =
        placementScanFrozen && isPlacementPatchVisible()

    fun isPlacementFallbackPatchVisible(): Boolean =
        false

    /** Move exploration patch only — no geometry/prepare/activeMode churn. */
    fun updatePlacementExplorationPatchPosition(
        hitPoint: ArPoint3D,
        sectionFloorY: Float?
    ) {
        if (!placementScanFrozen) return
        val node = placementPatchNode ?: return
        if (node.hidden) return
        applyPlacementPatchTransform(node, hitPoint, sectionFloorY)
    }

    private fun applyPlacementPatchTransform(
        node: SCNNode,
        hitPoint: ArPoint3D,
        sectionFloorY: Float?
    ) {
        val material = placementGridMaterial ?: return
        node.geometry = buildWorldAlignedPlacementPatchGeometry(
            worldCenterX = hitPoint.xMeters,
            worldCenterZ = hitPoint.zMeters,
            material = material
        )
        val visualY = (sectionFloorY ?: hitPoint.yMeters) + GRID_VISUAL_OFFSET_M
        node.position = SCNVector3Make(hitPoint.xMeters, visualY, hitPoint.zMeters)
    }

    fun hidePlacementExplorationPatch() {
        placementPatchNode?.hidden = true
    }

    /** Skip overlay transform updates for a few frames after relocalization / large camera gap. */
    fun requestOverlayTransformDebounce(frames: Int = OVERLAY_TRANSFORM_DEBOUNCE_FRAMES) {
        overlayTransformSkipFrames = frames.coerceAtLeast(overlayTransformSkipFrames)
    }

    fun tickOverlayTransformDebounce() {
        if (overlayTransformSkipFrames > 0) {
            overlayTransformSkipFrames--
        }
    }

    fun isOverlayTransformDebouncing(): Boolean = overlayTransformSkipFrames > 0

    /** Dim reference grid when ARKit frame stream is sparse or relocalizing. */
    fun applyReferenceOverlayDegraded(degraded: Boolean) {
        if (referenceOverlayDegraded == degraded) return
        referenceOverlayDegraded = degraded
        val alpha = if (degraded) GRID_ALPHA_DEGRADED else GRID_ALPHA_NORMAL
        overlays.values.forEach { overlay ->
            overlay.material.diffuse.contents = UIColor.whiteColor.colorWithAlphaComponent(alpha)
        }
    }

    fun syncPlacementPatch(
        rootNode: SCNNode,
        centerHit: CenterPlaneHit,
        sectionFloorY: Float?,
        show: Boolean
    ): Boolean {
        if (contourModeActive) return false
        prepare()
        val hitPoint = centerHit.placementFloorPoint(sectionFloorY) ?: run {
            placementPatchNode?.hidden = true
            return false
        }
        if (!show) {
            placementPatchNode?.hidden = true
            return false
        }
        val node = placementPatchNode ?: return false
        pruneDuplicateNamedNodes(rootNode, PLACEMENT_PATCH_NODE_NAME, keep = node)
        if (node.parentNode != rootNode) {
            node.removeFromParentNode()
            rootNode.addChildNode(node)
        }
        node.geometry?.firstMaterial = placementGridMaterial
        val visualY = (sectionFloorY ?: hitPoint.yMeters) + GRID_VISUAL_OFFSET_M
        node.position = SCNVector3Make(hitPoint.xMeters, visualY, hitPoint.zMeters)
        node.eulerAngles = SCNVector3Make(0f, 0f, 0f)
        node.hidden = false
        activeMode = GridSurfaceMode.RETICLE_ONLY
        return true
    }

    fun isPlacementPatchVisible(): Boolean =
        placementPatchNode?.let { !it.hidden } == true

    fun remove(anchorId: NSUUID) {
        if (placementScanFrozen) return
        asyncGridBuilder.cancel(anchorId)
        areas.remove(anchorId)
        elevationLock.clear(anchorId)
        removeOverlay(anchorId)
        if (!sectionFloorLocked && stickyFloorAnchorId == anchorId) {
            stickyFloorAnchorId = null
            cachedFloorWorldY = null
        }
        updateActiveMode()
    }

    fun area(anchorId: NSUUID): Float = areas[anchorId] ?: 0f

    fun estimatedFloorWorldY(): Float? = cachedFloorWorldY

    fun isLockedSectionAnchor(anchorId: NSUUID): Boolean =
        sectionFloorLocked && stickyFloorAnchorId == anchorId

    /** Reference grid is built and pinned — ignore further ARKit plane refine callbacks. */
    fun isPlacementReferenceFrozen(anchorId: NSUUID): Boolean {
        if (!placementScanFrozen || !sectionFloorLocked || stickyFloorAnchorId != anchorId) return false
        val overlay = overlays[anchorId] ?: return false
        return overlay.node.geometry != null
    }

    /** Pin scan floor level when contour placement starts — stops sticky Y drift mid-session. */
    fun lockSectionFloor(anchorId: NSUUID?, worldY: Float) {
        sectionFloorLocked = true
        overlayRotationFrozen = true
        if (anchorId != null) {
            stickyFloorAnchorId = anchorId
        }
        cachedFloorWorldY = worldY
    }

    private fun tryPinVisibleOverlays(sceneRoot: SCNNode?) {
        if (sceneRoot == null || overlaysPinnedToWorld) return
        overlaysPinnedToWorld = pinVisibleOverlaysToWorldFloor(sceneRoot)
    }

    private fun pinVisibleOverlaysToWorldFloor(sceneRoot: SCNNode): Boolean {
        val floorY = cachedFloorWorldY ?: return false
        val visualY = floorY + GRID_VISUAL_OFFSET_M
        var pinnedAny = false
        overlays.values.forEach { overlay ->
            if (overlay.node.hidden) return@forEach
            val worldTransform = overlay.node.worldTransform
            overlay.node.removeFromParentNode()
            sceneRoot.addChildNode(overlay.node)
            overlay.node.transform = worldTransform
            val pinnedPosition = overlay.node.position.useContents {
                SCNVector3Make(x.toFloat(), visualY, z.toFloat())
            }
            overlay.node.position = pinnedPosition
            overlay.node.hidden = false
            pinnedAny = true
        }
        if (pinnedAny) {
            activeMode = GridSurfaceMode.MULTI_SURFACE
        }
        return pinnedAny
    }

    fun activeMode(): GridSurfaceMode = activeMode

    fun overlayCount(): Int =
        overlays.count { !it.value.node.hidden } +
            (if (isReticlePatchVisible()) 1 else 0) +
            (if (isPlacementPatchVisible()) 1 else 0)

    fun visibleSurfaceCount(): Int = overlays.count { !it.value.node.hidden }

    fun isReticlePatchVisible(): Boolean =
        reticlePatchNode?.let { !it.hidden } == true

    private fun removeOverlay(anchorId: NSUUID) {
        overlays.remove(anchorId)?.node?.removeFromParentNode()
    }

    private fun updateOverlayGeometry(
        anchor: ARPlaneAnchor,
        anchorNode: SCNNode,
        sceneRoot: SCNNode?,
        force: Boolean
    ): Boolean {
        val bridgePtr = anchor.bridgePointer()
        val signature = pg_geometry_signature(bridgePtr)
        val area = pg_polygon_area(bridgePtr)
        if (area < MIN_RENDER_AREA_M2) return false

        areas[anchor.identifier] = area
        val overlay = overlays.getOrPut(anchor.identifier) {
            SurfaceOverlay(
                node = SCNNode().apply {
                    name = OVERLAY_NODE_NAME
                    hidden = true
                },
                material = createGridLineMaterial(),
                lastGeometrySignature = 0u,
                lastGeometryUpdateSeconds = 0.0
            )
        }

        val worldLockedParent = if (sectionFloorLocked) sceneRoot else null
        if (worldLockedParent != null) {
            removeOverlayNodesOn(anchorNode, keep = overlay.node)
            if (overlay.node.parentNode != worldLockedParent) {
                overlay.node.removeFromParentNode()
                worldLockedParent.addChildNode(overlay.node)
                overlay.lastGeometrySignature = 0u
            }
            pinOverlayNodeToAnchorWorldFloor(overlay.node, anchorNode)
            overlaysPinnedToWorld = true
        } else if (overlay.node.parentNode != anchorNode) {
            overlay.node.removeFromParentNode()
            removeOverlayNodesOn(anchorNode, keep = overlay.node)
            anchorNode.addChildNode(overlay.node)
            overlay.lastGeometrySignature = 0u
        } else {
            removeOverlayNodesOn(anchorNode, keep = overlay.node)
        }

        if (placementScanFrozen && sectionFloorLocked && overlay.node.geometry != null) {
            overlay.node.hidden = false
            return true
        }

        val now = CFAbsoluteTimeGetCurrent()
        val geometryStale =
            signature != overlay.lastGeometrySignature ||
                force ||
                now - overlay.lastGeometryUpdateSeconds >= ANCHOR_GEOMETRY_MIN_INTERVAL_S

        if (geometryStale) {
            updatePolygonSurfaceGeometry(overlay, anchor, signature)
            overlay.lastGeometryUpdateSeconds = now
        }

        overlay.node.hidden = false
        return true
    }

    private fun pinOverlayNodeToAnchorWorldFloor(node: SCNNode, anchorNode: SCNNode) {
        if (overlayTransformSkipFrames > 0) return
        val floorY = cachedFloorWorldY ?: return
        val visualY = floorY + GRID_VISUAL_OFFSET_M
        node.transform = anchorNode.worldTransform
        val pinnedPosition = node.position.useContents {
            SCNVector3Make(x.toFloat(), visualY, z.toFloat())
        }
        node.position = pinnedPosition
        if (placementScanFrozen && overlayRotationFrozen) {
            node.eulerAngles = SCNVector3Make(0f, 0f, 0f)
        }
    }

    private fun updateStickyFloor(ranked: List<ARPlaneAnchor>) {
        if (sectionFloorLocked) return
        val largest = ranked.firstOrNull() ?: return
        val largestArea = areas[largest.identifier] ?: return
        val largestY = HitTransformReader.worldPointFromAnchor(largest).yMeters
        val currentId = stickyFloorAnchorId
        if (currentId == null) {
            stickyFloorAnchorId = largest.identifier
            cachedFloorWorldY = largestY
            return
        }
        val cachedY = cachedFloorWorldY
        if (cachedY != null && abs(largestY - cachedY) > STICKY_FLOOR_Y_RESET_M) {
            stickyFloorAnchorId = largest.identifier
            cachedFloorWorldY = largestY
            return
        }
        val currentArea = areas[currentId] ?: 0f
        if (largest.identifier == currentId) {
            cachedFloorWorldY = largestY
            return
        }
        if (largestArea >= currentArea * FLOOR_SWITCH_AREA_RATIO) {
            stickyFloorAnchorId = largest.identifier
            cachedFloorWorldY = largestY
        }
    }

    private fun pickVisibleAnchors(
        ranked: List<ARPlaneAnchor>,
        scanContext: IosArScanSurfaceContext,
        floorArea: Float?,
        cullStats: OverlayCullStats,
        reticleAnchorId: NSUUID? = null
    ): List<ARPlaneAnchor> {
        if (ranked.isEmpty()) return emptyList()

        if (sectionFloorLocked) {
            val floorY = cachedFloorWorldY
            val preferred = listOfNotNull(
                stickyFloorAnchorId?.let { id -> ranked.firstOrNull { it.identifier == id } },
                reticleAnchorId?.let { id -> ranked.firstOrNull { it.identifier == id } },
                ranked.firstOrNull { isAllowedByLockedSectionFloor(it, floorY) }
            ).firstOrNull { anchor ->
                isEligibleOverlayAnchor(anchor, scanContext, floorArea, cullStats)
            }
            return preferred?.let { listOf(it) } ?: emptyList()
        }

        val selected = mutableListOf<ARPlaneAnchor>()
        val largest = ranked.first()
        val floorY = cachedFloorWorldY
        if (
            isEligibleOverlayAnchor(largest, scanContext, floorArea, cullStats) &&
            isAllowedByLockedSectionFloor(largest, floorY)
        ) {
            selected += largest
        }

        for (anchor in ranked.drop(1)) {
            if (selected.size >= MAX_SURFACE_OVERLAYS) break
            if (!isEligibleOverlayAnchor(anchor, scanContext, floorArea, cullStats)) continue
            if (!isAllowedByLockedSectionFloor(anchor, floorY)) continue
            if (floorY != null && !sectionFloorLocked) {
                val anchorY = HitTransformReader.worldPointFromAnchor(anchor).yMeters
                if (abs(anchorY - floorY) < SAME_LEVEL_Y_DELTA_M) continue
            }
            if (!sectionFloorLocked && isDuplicateOfSelected(anchor, selected)) continue
            selected += anchor
        }
        return selected
    }

    private fun isAllowedByLockedSectionFloor(anchor: ARPlaneAnchor, floorY: Float?): Boolean {
        if (!sectionFloorLocked || floorY == null) return true
        val anchorY = HitTransformReader.worldPointFromAnchor(anchor).yMeters
        return abs(anchorY - floorY) <= SAME_LEVEL_Y_DELTA_M
    }

    private fun isEligibleOverlayAnchor(
        anchor: ARPlaneAnchor,
        scanContext: IosArScanSurfaceContext,
        floorArea: Float?,
        cullStats: OverlayCullStats
    ): Boolean {
        if (!isPlausibleOverlayAnchor(anchor, scanContext.cameraWorldYMeters)) return false
        if (!isWithinOverlayDistance(anchor, scanContext.cameraWorldPosition)) {
            cullStats.distanceCulled++
            return false
        }
        if (shouldSuppressElevated(anchor, floorArea)) {
            cullStats.elevatedCulled++
            return false
        }
        if (shouldSuppressByClassification(anchor)) {
            cullStats.elevatedCulled++
            return false
        }
        return true
    }

    private fun shouldSuppressByClassification(anchor: ARPlaneAnchor): Boolean {
        if (pg_plane_is_floor_like(anchor.bridgePointer())) return false
        val classification = pg_plane_classification(anchor.bridgePointer())
        if (classification == PG_PLANE_CLASSIFICATION_FLOOR) return false

        val floorY = cachedFloorWorldY ?: return classification in NON_FLOOR_CLASSIFICATIONS
        val anchorY = HitTransformReader.worldPointFromAnchor(anchor).yMeters
        if (anchor.identifier == stickyFloorAnchorId) return false
        if (anchorY <= floorY + ELEVATED_SUPPRESS_DELTA_M) {
            return classification in NON_FLOOR_CLASSIFICATIONS
        }
        return true
    }

    private fun isWithinOverlayDistance(
        anchor: ARPlaneAnchor,
        cameraWorldPosition: ArPoint3D?
    ): Boolean {
        val camera = cameraWorldPosition ?: return true
        val anchorWorld = HitTransformReader.worldPointFromAnchor(anchor)
        val dx = anchorWorld.xMeters - camera.xMeters
        val dy = anchorWorld.yMeters - camera.yMeters
        val dz = anchorWorld.zMeters - camera.zMeters
        return sqrt(dx * dx + dy * dy + dz * dz) <= MAX_OVERLAY_DISTANCE_M
    }

    private fun shouldSuppressElevated(
        anchor: ARPlaneAnchor,
        floorArea: Float?
    ): Boolean {
        val floorY = cachedFloorWorldY ?: return false
        val area = floorArea ?: return false
        if (area < MIN_RENDER_AREA_M2) return false
        val anchorY = HitTransformReader.worldPointFromAnchor(anchor).yMeters
        if (anchorY <= floorY + ELEVATED_SUPPRESS_DELTA_M) return false
        val anchorArea = areas[anchor.identifier] ?: return false
        return anchorArea < area
    }

    private fun isPlausibleOverlayAnchor(
        anchor: ARPlaneAnchor,
        cameraWorldYMeters: Float?
    ): Boolean {
        val cameraY = cameraWorldYMeters ?: return true
        val anchorY = HitTransformReader.worldPointFromAnchor(anchor).yMeters
        if (anchorY > cameraY + MAX_PLANE_ABOVE_CAMERA_M) return false
        if (anchorY < cameraY - MAX_PLANE_BELOW_CAMERA_M) return false
        return true
    }

    private fun isDuplicateOfSelected(
        candidate: ARPlaneAnchor,
        selected: List<ARPlaneAnchor>
    ): Boolean {
        val candidateY = HitTransformReader.worldPointFromAnchor(candidate).yMeters
        for (other in selected) {
            val otherY = HitTransformReader.worldPointFromAnchor(other).yMeters
            if (abs(candidateY - otherY) < SAME_LEVEL_Y_DELTA_M) return true
        }
        return false
    }

    private fun updateActiveMode() {
        val surfaceCount = visibleSurfaceCount()
        val hasReticle = isReticlePatchVisible()
        activeMode = when {
            surfaceCount > 0 && hasReticle -> GridSurfaceMode.MULTI_WITH_RETICLE
            surfaceCount > 0 -> GridSurfaceMode.MULTI_SURFACE
            hasReticle -> GridSurfaceMode.RETICLE_ONLY
            else -> GridSurfaceMode.HIDDEN
        }
    }

    private fun shouldSuppressReticlePatch(
        centerHit: CenterPlaneHit,
        floorWorldY: Float?,
        largestPlaneAreaM2: Float
    ): Boolean {
        if (visibleSurfaceCount() == 0) return false
        val effectiveFloorY = floorWorldY ?: cachedFloorWorldY ?: return false
        if (largestPlaneAreaM2 < MIN_RENDER_AREA_M2) return false

        val hitY = centerHit.confirmedWorldFloorPoint()?.yMeters
            ?: centerHit.previewWorldFloorPoint()?.yMeters
        return hitY != null && abs(hitY - effectiveFloorY) <= ELEVATED_Y_DELTA_M
    }

    private fun pickReticleHit(
        centerHit: CenterPlaneHit,
        floorWorldY: Float?,
        scanContext: IosArScanSurfaceContext,
        largestPlaneAreaM2: Float
    ): ArWorldScnMatrix? {
        val effectiveFloorY = floorWorldY ?: cachedFloorWorldY
        val cameraY = scanContext.cameraWorldYMeters

        val confirmedMatrix = centerHit.confirmedWorldScnMatrix()
        if (confirmedMatrix != null &&
            isPlausibleReticleHit(centerHit.confirmedWorldFloorPoint(), effectiveFloorY, cameraY)
        ) {
            val confirmedY = centerHit.confirmedWorldFloorPoint()?.yMeters
            if (!isHitOnFloorLevel(confirmedY, effectiveFloorY)) {
                return confirmedMatrix
            }
        }

        if (scanContext.allowEstimatedPatch && visibleSurfaceCount() == 0) {
            val previewMatrix = centerHit.previewWorldScnMatrix()
            if (previewMatrix != null &&
                isPlausibleReticleHit(centerHit.previewWorldFloorPoint(), effectiveFloorY, cameraY)
            ) {
                return previewMatrix
            }
        }

        return null
    }

    private fun isHitOnFloorLevel(hitY: Float?, floorWorldY: Float?): Boolean {
        if (hitY == null || floorWorldY == null) return false
        return abs(hitY - floorWorldY) <= ELEVATED_Y_DELTA_M
    }

    private fun isPlausibleReticleHit(
        hitPoint: ArPoint3D?,
        floorWorldY: Float?,
        cameraWorldYMeters: Float?
    ): Boolean {
        val hitY = hitPoint?.yMeters ?: return false
        if (cameraWorldYMeters != null) {
            if (hitY > cameraWorldYMeters + MAX_RETICLE_ABOVE_CAMERA_M) return false
            if (hitY < cameraWorldYMeters - MAX_PLANE_BELOW_CAMERA_M) return false
        }
        if (floorWorldY == null) return true
        if (abs(hitY - floorWorldY) <= ELEVATED_Y_DELTA_M) return false
        if (hitY > floorWorldY + MAX_RETICLE_ABOVE_FLOOR_M) return false
        if (hitY < floorWorldY - MAX_RETICLE_BELOW_FLOOR_M) return false
        return true
    }

    private fun updatePolygonSurfaceGeometry(
        overlay: SurfaceOverlay,
        anchor: ARPlaneAnchor,
        signature: UInt
    ) {
        if (placementScanFrozen && sectionFloorLocked) return
        if (overlay.pendingBuildSignature == signature) return
        if (overlay.node.geometry != null && signature == overlay.lastGeometrySignature) return

        val boundary = copyPlaneBoundarySnapshot(anchor) ?: return
        val anchorId = anchor.identifier
        overlay.pendingBuildSignature = signature

        asyncGridBuilder.requestBuild(
            anchorId = anchorId,
            boundary = boundary,
            cellM = GRID_CELL_METERS,
            lineWidthM = GRID_LINE_WIDTH_M,
            boundaryLineWidthM = GRID_BOUNDARY_LINE_WIDTH_M,
            yM = 0f
        ) { geometry ->
            val liveOverlay = overlays[anchorId] ?: return@requestBuild
            liveOverlay.pendingBuildSignature = null
            if (geometry == null) return@requestBuild
            geometry.materials = listOf(liveOverlay.material)
            liveOverlay.node.geometry = geometry
            liveOverlay.lastGeometrySignature = signature
        }
    }

    private fun removeOverlayNodesOn(anchorNode: SCNNode, keep: SCNNode) {
        anchorNode.childNodes.forEach { child ->
            val node = child as? SCNNode ?: return@forEach
            if (node.name == OVERLAY_NODE_NAME && node != keep) {
                node.removeFromParentNode()
            }
        }
    }

    private fun pruneDuplicateNamedNodes(
        parent: SCNNode,
        name: String,
        keep: SCNNode
    ) {
        parent.childNodes.forEach { child ->
            val node = child as? SCNNode ?: return@forEach
            if (node.name == name && node != keep) {
                node.removeFromParentNode()
            }
        }
    }
}

private data class SurfaceOverlay(
    val node: SCNNode,
    val material: SCNMaterial,
    var lastGeometrySignature: UInt,
    var lastGeometryUpdateSeconds: Double,
    var pendingBuildSignature: UInt? = null
)

@OptIn(ExperimentalForeignApi::class)
private fun buildExtentGridLineGeometry(
    widthM: Float,
    depthM: Float,
    centerX: Float,
    centerZ: Float,
    material: SCNMaterial
): SCNGeometry? =
    pg_create_grid_line_geometry(
        widthM = widthM,
        depthM = depthM,
        centerX = centerX,
        centerZ = centerZ,
        cellM = GRID_CELL_METERS,
        lineWidthM = GRID_LINE_WIDTH_M,
        yM = 0f
    )?.apply {
        materials = listOf(material)
    }

@OptIn(ExperimentalForeignApi::class)
private fun buildWorldAlignedPlacementPatchGeometry(
    worldCenterX: Float,
    worldCenterZ: Float,
    material: SCNMaterial
): SCNGeometry? =
    pg_create_world_aligned_placement_patch_grid_geometry(
        patchSizeM = PLACEMENT_SURFACE_PATCH_M,
        worldCenterX = worldCenterX,
        worldCenterZ = worldCenterZ,
        cellM = PLACEMENT_PATCH_CELL_M,
        lineWidthM = PLACEMENT_PATCH_LINE_WIDTH_M,
        boundaryLineWidthM = PLACEMENT_PATCH_BOUNDARY_WIDTH_M,
        yM = 0f
    )?.apply {
        materials = listOf(material)
    }

@OptIn(ExperimentalForeignApi::class)
private val NON_FLOOR_CLASSIFICATIONS = setOf(
    PG_PLANE_CLASSIFICATION_TABLE,
    PG_PLANE_CLASSIFICATION_SEAT
)

@OptIn(ExperimentalForeignApi::class)
private fun createGridLineMaterial(): SCNMaterial =
    SCNMaterial().apply {
        diffuse.contents = UIColor.whiteColor.colorWithAlphaComponent(GRID_ALPHA_NORMAL)
        emission.contents = UIColor.whiteColor
        emission.intensity = 0.20
        lightingModelName = platform.SceneKit.SCNLightingModelConstant
        doubleSided = true
        readsFromDepthBuffer = true
        writesToDepthBuffer = true
    }

internal fun CenterPlaneHit.placementFloorPoint(sectionFloorY: Float?): ArPoint3D? {
    val confirmed = confirmedWorldFloorPoint()
    if (confirmed != null) {
        if (sectionFloorY != null &&
            abs(confirmed.yMeters - sectionFloorY) > SAME_LEVEL_Y_DELTA_M
        ) {
            return null
        }
        return confirmed
    }
    val preview = previewWorldFloorPoint()
    if (preview != null) {
        if (sectionFloorY != null &&
            abs(preview.yMeters - sectionFloorY) > SAME_LEVEL_Y_DELTA_M
        ) {
            return null
        }
        return preview
    }
    return null
}

internal fun shouldShowPlacementPatch(status: String): Boolean =
    status == "valid" || status == "preview"
