package com.example.arplitka.iosapp

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.COpaque
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.objcPtr
import kotlinx.cinterop.ptr
import kotlinx.cinterop.set
import kotlinx.cinterop.useContents
import kotlinx.cinterop.value
import platform.darwin.NSObject
import platform.ARKit.ARCamera
import platform.ARKit.ARHitTestResult
import platform.ARKit.ARHitTestResultTypeExistingPlaneUsingExtent
import platform.ARKit.ARPlaneAnchor
import platform.ARKit.ARPlaneAnchorAlignment
import platform.Foundation.NSUUID
import platform.SceneKit.SCNGeometry
import platform.SceneKit.SCNMaterial
import platform.SceneKit.SCNMatrix4FromMat4
import platform.SceneKit.SCNNode
import platform.SceneKit.SCNVector3Make
import platform.UIKit.UIColor
import kotlin.math.roundToInt
import com.example.arplitka.iosapp.bridge.PG_DOT_BOUNDARY_EXTENT
import com.example.arplitka.iosapp.bridge.PG_DOT_BOUNDARY_POLYGON
import com.example.arplitka.iosapp.bridge.pg_anchor_has_polygon_boundary
import com.example.arplitka.iosapp.bridge.pg_collect_window_dot_points
import com.example.arplitka.iosapp.bridge.pg_create_dot_mesh_from_points
import com.example.arplitka.iosapp.bridge.pg_create_dot_mesh_geometry
import com.example.arplitka.iosapp.bridge.pg_create_dot_mesh_local_disc
import com.example.arplitka.iosapp.bridge.pg_create_preview_dot_mesh_geometry
import com.example.arplitka.iosapp.bridge.pg_world_xz_on_anchor
import com.example.arplitka.iosapp.bridge.pg_geometry_signature
import com.example.arplitka.iosapp.bridge.pg_geometry_signature_extent
import com.example.arplitka.iosapp.bridge.pg_local_point_in_polygon
import com.example.arplitka.iosapp.bridge.pg_local_point_in_render_boundary
import com.example.arplitka.iosapp.bridge.pg_polygon_area

@OptIn(ExperimentalForeignApi::class)
internal fun ARPlaneAnchor.isHorizontalTracking(): Boolean =
    alignment == ARPlaneAnchorAlignment.ARPlaneAnchorAlignmentHorizontal

internal enum class PlaneDotMeshSource {
    HIT,
    EXTENT,
    POLYGON,
    ACCUMULATE;

    val debugLabel: String
        get() = when (this) {
            HIT -> "hit"
            EXTENT -> "extent"
            POLYGON -> "polygon"
            ACCUMULATE -> "accumulate"
        }
}

internal data class IosPlaneGeometry(
    val meshGeometry: SCNGeometry,
    val dotCount: Int,
    val area: Float,
    val fingerprint: UInt,
    val source: PlaneDotMeshSource
)

@OptIn(ExperimentalForeignApi::class)
internal object HitTransformReader {
    private val scratchNode = SCNNode()

    fun localFloorPoint(hitResult: ARHitTestResult): Pair<Float, Float> {
        scratchNode.transform = SCNMatrix4FromMat4(hitResult.localTransform)
        return scratchNode.position.useContents { x.toFloat() to z.toFloat() }
    }

    fun worldFloorPoint(hitResult: ARHitTestResult): com.example.arplitka.shared.ar.contracts.model.ArPoint3D {
        scratchNode.transform = SCNMatrix4FromMat4(hitResult.worldTransform)
        return scratchNode.position.useContents {
            com.example.arplitka.shared.ar.contracts.model.ArPoint3D(
                xMeters = x.toFloat(),
                yMeters = y.toFloat(),
                zMeters = z.toFloat()
            )
        }
    }

    fun worldPointFromAnchor(anchor: platform.ARKit.ARAnchor): com.example.arplitka.shared.ar.contracts.model.ArPoint3D {
        scratchNode.transform = SCNMatrix4FromMat4(anchor.transform)
        return scratchNode.position.useContents {
            com.example.arplitka.shared.ar.contracts.model.ArPoint3D(
                xMeters = x.toFloat(),
                yMeters = y.toFloat(),
                zMeters = z.toFloat()
            )
        }
    }

    fun worldPointFromCamera(camera: ARCamera): com.example.arplitka.shared.ar.contracts.model.ArPoint3D {
        scratchNode.transform = SCNMatrix4FromMat4(camera.transform)
        return scratchNode.position.useContents {
            com.example.arplitka.shared.ar.contracts.model.ArPoint3D(
                xMeters = x.toFloat(),
                yMeters = y.toFloat(),
                zMeters = z.toFloat()
            )
        }
    }

    fun worldXZOnAnchor(
        anchor: platform.ARKit.ARAnchor,
        world: com.example.arplitka.shared.ar.contracts.model.ArPoint3D
    ): Pair<Float, Float> =
        memScoped {
            val localX = alloc<FloatVar>()
            val localZ = alloc<FloatVar>()
            val ok = pg_world_xz_on_anchor(
                anchorPtr = anchor.bridgePointer(),
                worldX = world.xMeters,
                worldY = world.yMeters,
                worldZ = world.zMeters,
                outLocalX = localX.ptr,
                outLocalZ = localZ.ptr
            )
            if (!ok) {
                0f to 0f
            } else {
                localX.value to localZ.value
            }
        }

}

@OptIn(ExperimentalForeignApi::class)
internal fun createFloorDotMaterial(): SCNMaterial =
    SCNMaterial().apply {
        diffuse.contents = UIColor.whiteColor.colorWithAlphaComponent(0.92)
        lightingModelName = platform.SceneKit.SCNLightingModelConstant
        doubleSided = true
        readsFromDepthBuffer = true
        writesToDepthBuffer = true
    }

private fun dotLocalY(): Float = GRID_VISUAL_OFFSET_M

/**
 * ARKit (especially indoors) often nudges plane anchors upward while refining.
 * Track the lowest seen raycast floor height per anchor and pin the dot grid to that level.
 */
@OptIn(ExperimentalForeignApi::class)
internal class PlaneDotElevationLock {
    private val floorWorldYByAnchor = mutableMapOf<NSUUID, Float>()

    fun clear(anchorId: NSUUID) {
        floorWorldYByAnchor.remove(anchorId)
    }

    fun clearAll() {
        floorWorldYByAnchor.clear()
    }

    /** Call each frame when a center hit is available; keeps the lowest floor height seen. */
    fun registerFloorSample(anchorId: NSUUID, worldY: Float) {
        val previous = floorWorldYByAnchor[anchorId]
        floorWorldYByAnchor[anchorId] = if (previous == null) worldY else minOf(previous, worldY)
    }

    /** Local Y offset so viz stays at the lowest seen floor when ARKit nudges the anchor up. */
    fun lockedLocalYOffsetM(anchor: ARPlaneAnchor, raycastWorldY: Float?): Float {
        val anchorWorldY = HitTransformReader.worldPointFromAnchor(anchor).yMeters
        if (raycastWorldY != null) {
            registerFloorSample(anchor.identifier, raycastWorldY)
            registerFloorSample(anchor.identifier, anchorWorldY)
        }
        val lockedFloorY = floorWorldYByAnchor[anchor.identifier]
            ?: raycastWorldY
            ?: anchorWorldY
        return lockedFloorY - anchorWorldY
    }

    fun apply(anchorNode: SCNNode, anchor: ARPlaneAnchor, raycastWorldY: Float?) {
        val grid = findDotGrid(anchorNode) ?: return
        val localY = dotLocalY() + lockedLocalYOffsetM(anchor, raycastWorldY)
        grid.position = SCNVector3Make(0f, localY, 0f)
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun ARPlaneAnchor.hasPolygonBoundary(): Boolean =
    pg_anchor_has_polygon_boundary(bridgePointer())

@OptIn(ExperimentalForeignApi::class)
internal fun selectAccumulateBoundaryMode(
    anchor: ARPlaneAnchor,
    polygonStableFrames: Int
): PlaneDotMeshSource =
    if (anchor.hasPolygonBoundary() && polygonStableFrames >= POLYGON_STABLE_FRAME_THRESHOLD) {
        PlaneDotMeshSource.POLYGON
    } else {
        PlaneDotMeshSource.EXTENT
    }

/**
 * Remembers scanned floor cells per plane anchor while the user walks around.
 */
internal class PlaneDotBucketAccumulator(
    private val stepM: Float = GRID_STEP_M,
    private val stampRadiusCells: Int = ACCUMULATE_STAMP_RADIUS_CELLS,
    private val maxBucketsPerAnchor: Int = MAX_ACCUMULATED_BUCKETS
) {
    private val bucketsByAnchor = mutableMapOf<NSUUID, LinkedHashSet<Long>>()

    fun clearAll() {
        bucketsByAnchor.clear()
    }

    fun clear(anchorId: NSUUID) {
        bucketsByAnchor.remove(anchorId)
    }

    fun remove(anchorId: NSUUID) {
        bucketsByAnchor.remove(anchorId)
    }

    fun bucketCount(anchorId: NSUUID): Int =
        bucketsByAnchor[anchorId]?.size ?: 0

    fun recordScan(anchorId: NSUUID, localX: Float, localZ: Float) {
        val buckets = bucketsByAnchor.getOrPut(anchorId) { LinkedHashSet() }
        for (cellX in -stampRadiusCells..stampRadiusCells) {
            for (cellZ in -stampRadiusCells..stampRadiusCells) {
                val x = bucketCoordinate(localX, cellX)
                val z = bucketCoordinate(localZ, cellZ)
                buckets.add(packBucketKey(x, z))
                trimBucketsIfNeeded(buckets)
            }
        }
    }

    fun fingerprint(anchorId: NSUUID, boundarySignature: UInt): UInt {
        val buckets = bucketsByAnchor[anchorId] ?: return boundarySignature
        var hash = boundarySignature * 31u + buckets.size.toUInt()
        for (key in buckets) {
            hash = hash xor key.toUInt()
            hash = hash * 31u + (key shr 32).toUInt()
        }
        return hash
    }

    @OptIn(ExperimentalForeignApi::class)
    fun visibleBucketPoints(
        anchor: ARPlaneAnchor,
        boundaryMode: PlaneDotMeshSource
    ): List<Pair<Float, Float>> {
        val boundaryKind = when (boundaryMode) {
            PlaneDotMeshSource.POLYGON -> PG_DOT_BOUNDARY_POLYGON
            else -> PG_DOT_BOUNDARY_EXTENT
        }
        val buckets = bucketsByAnchor[anchor.identifier] ?: return emptyList()
        return buckets.mapNotNull { key ->
            val (x, z) = unpackBucketKey(key)
            if (pg_local_point_in_render_boundary(anchor.bridgePointer(), boundaryKind, x, z)) {
                x to z
            } else {
                null
            }
        }
    }

    private fun bucketCoordinate(local: Float, cellOffset: Int): Float =
        (gridBucketIndex(local) + cellOffset) * stepM

    private fun gridBucketIndex(value: Float): Int =
        (value / stepM).roundToInt()

    private fun packBucketKey(localX: Float, localZ: Float): Long =
        packDotGridKey(localX, localZ)

    private fun unpackBucketKey(key: Long): Pair<Float, Float> {
        val bucketX = (key shr 32).toInt()
        val bucketZ = (key and 0xFFFF_FFFFL).toInt()
        return bucketX * stepM to bucketZ * stepM
    }

    private fun trimBucketsIfNeeded(buckets: LinkedHashSet<Long>) {
        while (buckets.size > maxBucketsPerAnchor) {
            val oldest = buckets.first()
            buckets.remove(oldest)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun buildCombinedPlaneGeometry(
    anchor: ARPlaneAnchor,
    accumulator: PlaneDotBucketAccumulator,
    boundaryMode: PlaneDotMeshSource,
    centerLocal: Pair<Float, Float>?,
    visibleRadiusM: Float
): IosPlaneGeometry? {
    val points = mergeDotDisplayPoints(
        anchor = anchor,
        accumulator = accumulator,
        boundaryMode = boundaryMode,
        centerLocal = centerLocal,
        visibleRadiusM = visibleRadiusM
    )
    if (points.isEmpty()) return null
    val mesh = PlaneGeometryBridge.generateDotMeshFromPoints(points) ?: return null
    val area = PlaneGeometryBridge.polygonArea(anchor)
    val fingerprint = combinedDisplayFingerprint(
        anchor = anchor,
        accumulator = accumulator,
        boundaryMode = boundaryMode,
        centerLocal = centerLocal,
        visibleRadiusM = visibleRadiusM
    )
    return IosPlaneGeometry(
        meshGeometry = mesh,
        dotCount = PlaneGeometryBridge.lastGeneratedDotCount,
        area = area,
        fingerprint = fingerprint,
        source = PlaneDotMeshSource.ACCUMULATE
    )
}

/** Lighter mesh while placing contour points: live window only, no bucket accumulate. */
@OptIn(ExperimentalForeignApi::class)
internal fun buildLiveWindowPlaneGeometry(
    anchor: ARPlaneAnchor,
    boundaryMode: PlaneDotMeshSource,
    centerLocal: Pair<Float, Float>,
    visibleRadiusM: Float
): IosPlaneGeometry? {
    val points = PlaneGeometryBridge.collectWindowDotPoints(
        anchor = anchor,
        boundaryMode = boundaryMode,
        refX = centerLocal.first,
        refZ = centerLocal.second,
        maxRadiusM = visibleRadiusM
    )
    if (points.isEmpty()) return null
    val mesh = PlaneGeometryBridge.generateDotMeshFromPoints(points) ?: return null
    val fingerprint = combinedDisplayFingerprint(
        anchor = anchor,
        accumulator = PlaneDotBucketAccumulator(),
        boundaryMode = boundaryMode,
        centerLocal = centerLocal,
        visibleRadiusM = visibleRadiusM
    )
    return IosPlaneGeometry(
        meshGeometry = mesh,
        dotCount = PlaneGeometryBridge.lastGeneratedDotCount,
        area = PlaneGeometryBridge.polygonArea(anchor),
        fingerprint = fingerprint,
        source = PlaneDotMeshSource.HIT
    )
}

@OptIn(ExperimentalForeignApi::class)
internal fun combinedDisplayFingerprint(
    anchor: ARPlaneAnchor,
    accumulator: PlaneDotBucketAccumulator,
    boundaryMode: PlaneDotMeshSource,
    centerLocal: Pair<Float, Float>?,
    visibleRadiusM: Float
): UInt {
    val boundarySignature = when (boundaryMode) {
        PlaneDotMeshSource.POLYGON -> pg_geometry_signature(anchor.bridgePointer())
        else -> pg_geometry_signature_extent(anchor.bridgePointer())
    }
    var fingerprint = accumulator.fingerprint(anchor.identifier, boundarySignature)
    if (centerLocal != null) {
        fingerprint = fingerprint * 31u + centerFingerprintBucket(centerLocal.first).toUInt()
        fingerprint = fingerprint * 31u + centerFingerprintBucket(centerLocal.second).toUInt()
        fingerprint = fingerprint * 31u + visibleRadiusM.toRawBits().toUInt()
    }
    return fingerprint
}

@OptIn(ExperimentalForeignApi::class)
private fun mergeDotDisplayPoints(
    anchor: ARPlaneAnchor,
    accumulator: PlaneDotBucketAccumulator,
    boundaryMode: PlaneDotMeshSource,
    centerLocal: Pair<Float, Float>?,
    visibleRadiusM: Float
): List<Pair<Float, Float>> {
    val uniqueKeys = LinkedHashSet<Long>()
    val merged = ArrayList<Pair<Float, Float>>(accumulator.bucketCount(anchor.identifier) + 64)

    fun addPoint(point: Pair<Float, Float>) {
        val key = packDotGridKey(point.first, point.second)
        if (uniqueKeys.add(key)) {
            merged.add(point)
        }
    }

    accumulator.visibleBucketPoints(anchor, boundaryMode).forEach(::addPoint)

    if (centerLocal != null) {
        PlaneGeometryBridge.collectWindowDotPoints(
            anchor = anchor,
            boundaryMode = boundaryMode,
            refX = centerLocal.first,
            refZ = centerLocal.second,
            maxRadiusM = visibleRadiusM
        ).forEach(::addPoint)
    }

    return merged
}

private fun packDotGridKey(localX: Float, localZ: Float): Long {
    val bucketX = gridBucketIndex(localX).toLong()
    val bucketZ = gridBucketIndex(localZ).toLong() and 0xFFFF_FFFFL
    return (bucketX shl 32) or bucketZ
}

private fun gridBucketIndex(value: Float): Int =
    (value / GRID_STEP_M).roundToInt()

@OptIn(ExperimentalForeignApi::class)
internal fun ARPlaneAnchor.readPlaneGeometry(
    centerLocal: Pair<Float, Float>?,
    visibleRadiusM: Float,
    source: PlaneDotMeshSource,
    sourceTag: UInt = source.ordinal.toUInt()
): IosPlaneGeometry? {
    val refX = centerLocal?.first ?: 0f
    val refZ = centerLocal?.second ?: 0f
    val boundaryMode = when (source) {
        PlaneDotMeshSource.POLYGON -> PG_DOT_BOUNDARY_POLYGON
        PlaneDotMeshSource.EXTENT -> PG_DOT_BOUNDARY_EXTENT
        PlaneDotMeshSource.HIT, PlaneDotMeshSource.ACCUMULATE -> return null
    }
    val mesh = PlaneGeometryBridge.generateDotMeshGeometry(
        anchor = this,
        boundaryMode = boundaryMode,
        refX = refX,
        refZ = refZ,
        maxRadiusM = visibleRadiusM
    ) ?: return null
    val area = PlaneGeometryBridge.polygonArea(this)
    if (area <= 0f) return null
    val fingerprint = computeViewFingerprint(centerLocal, visibleRadiusM, source, sourceTag)
    return IosPlaneGeometry(
        meshGeometry = mesh,
        dotCount = PlaneGeometryBridge.lastGeneratedDotCount,
        area = area,
        fingerprint = fingerprint,
        source = source
    )
}

@OptIn(ExperimentalForeignApi::class)
internal fun buildHitDiscGeometry(
    centerLocal: Pair<Float, Float>,
    radiusM: Float = HIT_DOT_GRID_RADIUS_M
): IosPlaneGeometry? {
    val mesh = PlaneGeometryBridge.generateLocalDiscMeshGeometry(
        centerX = centerLocal.first,
        centerZ = centerLocal.second,
        radiusM = radiusM
    ) ?: return null
    val fingerprint = hitDiscFingerprint(centerLocal, radiusM)
    return IosPlaneGeometry(
        meshGeometry = mesh,
        dotCount = PlaneGeometryBridge.lastGeneratedDotCount,
        area = 0f,
        fingerprint = fingerprint,
        source = PlaneDotMeshSource.HIT
    )
}

@OptIn(ExperimentalForeignApi::class)
internal fun ARPlaneAnchor.computeViewFingerprint(
    centerLocal: Pair<Float, Float>?,
    visibleRadiusM: Float,
    source: PlaneDotMeshSource,
    sourceTag: UInt = source.ordinal.toUInt()
): UInt {
    val anchorSignature = when (source) {
        PlaneDotMeshSource.EXTENT -> pg_geometry_signature_extent(bridgePointer())
        PlaneDotMeshSource.POLYGON -> pg_geometry_signature(bridgePointer())
        PlaneDotMeshSource.HIT, PlaneDotMeshSource.ACCUMULATE -> 0u
    }
    var signature = anchorSignature * 31u + sourceTag
    if (centerLocal != null) {
        signature = signature * 31u + centerFingerprintBucket(centerLocal.first).toUInt()
        signature = signature * 31u + centerFingerprintBucket(centerLocal.second).toUInt()
        signature = signature * 31u + visibleRadiusM.toRawBits().toUInt()
    }
    return signature
}

internal fun hitDiscFingerprint(centerLocal: Pair<Float, Float>, radiusM: Float): UInt {
    var signature = PlaneDotMeshSource.HIT.ordinal.toUInt()
    signature = signature * 31u + centerFingerprintBucket(centerLocal.first).toUInt()
    signature = signature * 31u + centerFingerprintBucket(centerLocal.second).toUInt()
    signature = signature * 31u + radiusM.toRawBits().toUInt()
    return signature
}

@OptIn(ExperimentalForeignApi::class)
internal fun ARPlaneAnchor.containsLocalPoint(localX: Float, localZ: Float): Boolean =
    PlaneGeometryBridge.isLocalPointInPolygon(this, localX, localZ)

@OptIn(ExperimentalForeignApi::class)
private object PlaneGeometryBridge {
    private val outDotCount = nativeHeap.allocArray<IntVar>(1)
    var lastGeneratedDotCount: Int = 0
        private set

    fun isLocalPointInPolygon(anchor: ARPlaneAnchor, localX: Float, localZ: Float): Boolean =
        pg_local_point_in_polygon(anchor.bridgePointer(), localX, localZ)

    fun polygonArea(anchor: ARPlaneAnchor): Float =
        pg_polygon_area(anchor.bridgePointer())

    private val windowPointBufferX = nativeHeap.allocArray<FloatVar>(MAX_GENERATED_DOT_POINTS)
    private val windowPointBufferZ = nativeHeap.allocArray<FloatVar>(MAX_GENERATED_DOT_POINTS)

    fun collectWindowDotPoints(
        anchor: ARPlaneAnchor,
        boundaryMode: PlaneDotMeshSource,
        refX: Float,
        refZ: Float,
        maxRadiusM: Float
    ): List<Pair<Float, Float>> {
        val boundaryKind = when (boundaryMode) {
            PlaneDotMeshSource.POLYGON -> PG_DOT_BOUNDARY_POLYGON
            else -> PG_DOT_BOUNDARY_EXTENT
        }
        val count = pg_collect_window_dot_points(
            planeAnchor = anchor.bridgePointer(),
            boundaryMode = boundaryKind,
            stepM = GRID_STEP_M,
            refX = refX,
            refZ = refZ,
            maxRadiusM = maxRadiusM,
            outLocalX = windowPointBufferX,
            outLocalZ = windowPointBufferZ,
            maxPoints = MAX_GENERATED_DOT_POINTS
        )
        if (count <= 0) return emptyList()
        return List(count) { index ->
            windowPointBufferX[index] to windowPointBufferZ[index]
        }
    }

    fun generateDotMeshGeometry(
        anchor: ARPlaneAnchor,
        boundaryMode: Int,
        refX: Float,
        refZ: Float,
        maxRadiusM: Float
    ): SCNGeometry? {
        outDotCount[0] = 0
        val geometry = pg_create_dot_mesh_geometry(
            planeAnchor = anchor.bridgePointer(),
            boundaryMode = boundaryMode,
            stepM = GRID_STEP_M,
            refX = refX,
            refZ = refZ,
            maxRadiusM = maxRadiusM,
            dotRadiusM = FLOOR_DOT_RADIUS_M,
            yOffsetM = dotLocalY(),
            maxPoints = MAX_GENERATED_DOT_POINTS,
            outDotCount = outDotCount
        )
        lastGeneratedDotCount = outDotCount[0]
        return geometry
    }

    fun generateLocalDiscMeshGeometry(
        centerX: Float,
        centerZ: Float,
        radiusM: Float
    ): SCNGeometry? {
        outDotCount[0] = 0
        val geometry = pg_create_dot_mesh_local_disc(
            centerX = centerX,
            centerZ = centerZ,
            radiusM = radiusM,
            stepM = GRID_STEP_M,
            dotRadiusM = FLOOR_DOT_RADIUS_M,
            yOffsetM = dotLocalY(),
            outDotCount = outDotCount
        )
        lastGeneratedDotCount = outDotCount[0]
        return geometry
    }

    fun generateDotMeshFromPoints(points: List<Pair<Float, Float>>): SCNGeometry? {
        if (points.isEmpty()) return null
        outDotCount[0] = 0
        val xs = nativeHeap.allocArray<FloatVar>(points.size)
        val zs = nativeHeap.allocArray<FloatVar>(points.size)
        points.forEachIndexed { index, (x, z) ->
            xs[index] = x
            zs[index] = z
        }
        val geometry = pg_create_dot_mesh_from_points(
            localX = xs,
            localZ = zs,
            pointCount = points.size,
            dotRadiusM = FLOOR_DOT_RADIUS_M,
            yOffsetM = dotLocalY(),
            outDotCount = outDotCount
        )
        lastGeneratedDotCount = outDotCount[0]
        return geometry
    }

    fun generatePreviewDotMeshGeometry(): Pair<SCNGeometry, Int>? {
        outDotCount[0] = 0
        val geometry = pg_create_preview_dot_mesh_geometry(
            radiusM = HIT_DOT_GRID_RADIUS_M,
            stepM = GRID_STEP_M,
            dotRadiusM = FLOOR_DOT_RADIUS_M,
            yOffsetM = dotLocalY(),
            outDotCount = outDotCount
        ) ?: return null
        return geometry to outDotCount[0]
    }
}

private fun centerFingerprintBucket(value: Float): Int =
    (value / CENTER_FINGERPRINT_STEP_M).roundToInt()

@OptIn(ExperimentalForeignApi::class)
internal fun NSObject.bridgePointer() =
    interpretCPointer<COpaque>(objcPtr())

@OptIn(ExperimentalForeignApi::class)
internal fun syncPlaneGeometryDots(
    parentNode: SCNNode,
    geometry: IosPlaneGeometry,
    dotMaterial: SCNMaterial,
    previousFingerprint: UInt?
) {
    val grid = findOrCreateDotGrid(parentNode)
    grid.hidden = false

    if (previousFingerprint == geometry.fingerprint && grid.geometry != null) {
        return
    }

    removeChildDotNodes(grid)
    grid.geometry = geometry.meshGeometry
    grid.geometry?.materials = listOf(dotMaterial)
}

@OptIn(ExperimentalForeignApi::class)
internal fun hidePlaneDotGrid(parentNode: SCNNode) {
    findDotGrid(parentNode)?.hidden = true
}

@OptIn(ExperimentalForeignApi::class)
internal fun syncPreviewDotGrid(
    rootNode: SCNNode,
    centerHit: CenterPlaneHit,
    dotMaterial: SCNMaterial
): Int {
    val worldMatrix = centerHit.previewWorldScnMatrix() ?: return 0
    val grid = findOrCreatePreviewGrid(rootNode)
    grid.hidden = false
    grid.transform = worldMatrix

    if (grid.geometry == null) {
        PlaneGeometryBridge.generatePreviewDotMeshGeometry()?.let { (geometry, dotCount) ->
            removeChildDotNodes(grid)
            grid.geometry = geometry
            grid.geometry?.materials = listOf(dotMaterial)
            previewDotCount = dotCount
        }
    }
    return previewDotCount
}

internal fun hidePreviewDotGrid(rootNode: SCNNode) {
    findPreviewGrid(rootNode)?.hidden = true
}

internal fun activePlaneDotNodeCount(parentNode: SCNNode): Int =
    findDotGrid(parentNode)?.takeIf { !it.hidden && it.geometry != null }?.let { 1 } ?: 0

internal fun activePreviewDotNodeCount(rootNode: SCNNode): Int =
    findPreviewGrid(rootNode)?.takeIf { !it.hidden && it.geometry != null }?.let { 1 } ?: 0

internal fun removePlaneDotGrid(parentNode: SCNNode) {
    findDotGrid(parentNode)?.removeFromParentNode()
}

@OptIn(ExperimentalForeignApi::class)
private fun findOrCreateDotGrid(parentNode: SCNNode): SCNNode {
    findDotGrid(parentNode)?.let { return it }
    return SCNNode().also { grid ->
        grid.name = PLANE_GRID_NODE_NAME
        parentNode.addChildNode(grid)
    }
}

private fun findDotGrid(parentNode: SCNNode): SCNNode? =
    parentNode.childNodes.firstOrNull { node ->
        (node as? SCNNode)?.name == PLANE_GRID_NODE_NAME
    } as? SCNNode

@OptIn(ExperimentalForeignApi::class)
private fun findOrCreatePreviewGrid(rootNode: SCNNode): SCNNode {
    findPreviewGrid(rootNode)?.let { return it }
    return SCNNode().also { grid ->
        grid.name = PREVIEW_GRID_NODE_NAME
        rootNode.addChildNode(grid)
    }
}

private fun findPreviewGrid(rootNode: SCNNode): SCNNode? =
    rootNode.childNodes.firstOrNull { node ->
        (node as? SCNNode)?.name == PREVIEW_GRID_NODE_NAME
    } as? SCNNode

private fun removeChildDotNodes(grid: SCNNode) {
    grid.childNodes.forEach { node ->
        (node as? SCNNode)?.removeFromParentNode()
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun List<*>.firstHorizontalFloorHitResult(): ARHitTestResult? =
    closestHorizontalFloorHitResult()

/** Closest horizontal plane under the reticle (ARKit returns hits sorted by distance). */
@OptIn(ExperimentalForeignApi::class)
internal fun List<*>.closestHorizontalFloorHitResult(): ARHitTestResult? {
    var best: ARHitTestResult? = null
    var bestDistance = Double.MAX_VALUE
    for (item in this) {
        val result = item as? ARHitTestResult ?: continue
        val anchor = result.anchor as? ARPlaneAnchor ?: continue
        if (!anchor.isHorizontalTracking()) continue
        val distance = result.distance
        if (distance < bestDistance) {
            bestDistance = distance
            best = result
        }
    }
    return best
}

internal fun List<*>.firstEstimatedHorizontalFloorHitResult(): ARHitTestResult? =
    closestEstimatedHorizontalFloorHitResult()

/** Closest estimated horizontal plane (same idea as confirmed — avoids random far hits in air). */
@OptIn(ExperimentalForeignApi::class)
internal fun List<*>.closestEstimatedHorizontalFloorHitResult(): ARHitTestResult? {
    var best: ARHitTestResult? = null
    var bestDistance = Double.MAX_VALUE
    for (item in this) {
        val result = item as? ARHitTestResult ?: continue
        val anchor = result.anchor as? ARPlaneAnchor
        if (anchor != null && !anchor.isHorizontalTracking()) continue
        val distance = result.distance
        if (distance < bestDistance) {
            bestDistance = distance
            best = result
        }
    }
    return best
}

internal typealias PlaneFingerprints = MutableMap<NSUUID, UInt>
internal typealias PlaneAreas = MutableMap<NSUUID, Float>
internal typealias PlaneDotCounts = MutableMap<NSUUID, Int>
internal typealias PlanePolygonStableFrames = MutableMap<NSUUID, Int>

/**
 * Keeps the last focus for [graceFramesWithoutHit] frames when the reticle leaves all planes.
 */
internal class FocusedPlaneTracker(
    private val graceFramesWithoutHit: Int = FOCUS_GRACE_FRAMES
) {
    private var focusedAnchorId: NSUUID? = null
    private var framesWithoutHit: Int = 0

    fun update(centerAnchorId: NSUUID?): NSUUID? {
        if (centerAnchorId != null) {
            framesWithoutHit = 0
            focusedAnchorId = centerAnchorId
        } else {
            framesWithoutHit++
            if (framesWithoutHit > graceFramesWithoutHit) {
                focusedAnchorId = null
            }
        }
        return focusedAnchorId
    }

    fun isInGracePeriod(): Boolean =
        focusedAnchorId != null && framesWithoutHit in 1..graceFramesWithoutHit

    fun reset() {
        focusedAnchorId = null
        framesWithoutHit = 0
    }
}

internal const val VISIBLE_DOT_RADIUS_M = 2.0f
internal const val HIT_DOT_GRID_RADIUS_M = 0.55f
internal const val FOCUS_GRACE_FRAMES = 4
internal const val GRID_STEP_M = 0.18f
internal const val GRID_VISUAL_OFFSET_M = 0.0005f
internal const val FLOOR_DOT_RADIUS_M = 0.013f
internal const val MIN_FLOOR_AREA_M2 = 0.15f
internal const val PLANE_GRID_NODE_NAME = "plane-grid"
private const val PREVIEW_GRID_NODE_NAME = "plane-preview-grid"
internal const val CENTER_FINGERPRINT_STEP_M = 0.18f
private const val MAX_GENERATED_DOT_POINTS = 768
private const val MAX_ACCUMULATED_BUCKETS = 1024
private const val ACCUMULATE_STAMP_RADIUS_CELLS = 1
private const val POLYGON_STABLE_FRAME_THRESHOLD = 3
internal const val MESH_REBUILD_MIN_INTERVAL_SECONDS = 0.08
private var previewDotCount: Int = 0
