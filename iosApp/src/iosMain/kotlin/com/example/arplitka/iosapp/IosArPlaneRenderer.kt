package com.example.arplitka.iosapp

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.COpaque
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.get
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.objcPtr
import kotlinx.cinterop.useContents
import platform.darwin.NSObject
import platform.ARKit.ARHitTestResult
import platform.ARKit.ARHitTestResultTypeExistingPlaneUsingExtent
import platform.ARKit.ARPlaneAnchor
import platform.ARKit.ARPlaneAnchorAlignment
import platform.Foundation.NSUUID
import platform.SceneKit.SCNMaterial
import platform.SceneKit.SCNMatrix4FromMat4
import platform.SceneKit.SCNNode
import platform.SceneKit.SCNCylinder
import platform.SceneKit.SCNVector3Make
import platform.UIKit.UIColor
import kotlin.math.roundToInt
import com.example.arplitka.iosapp.bridge.pg_generate_dot_points
import com.example.arplitka.iosapp.bridge.pg_geometry_signature
import com.example.arplitka.iosapp.bridge.pg_local_point_in_polygon
import com.example.arplitka.iosapp.bridge.pg_polygon_area

@OptIn(ExperimentalForeignApi::class)
internal fun ARPlaneAnchor.isHorizontalTracking(): Boolean =
    alignment == ARPlaneAnchorAlignment.ARPlaneAnchorAlignmentHorizontal

internal data class PlanePoint(val x: Float, val z: Float)

internal data class IosPlaneGeometry(
    val dots: List<PlanePoint>,
    val area: Float,
    val fingerprint: UInt
)

@OptIn(ExperimentalForeignApi::class)
internal object HitTransformReader {
    private val scratchNode = SCNNode()

    fun localFloorPoint(hitResult: ARHitTestResult): Pair<Float, Float> {
        scratchNode.transform = SCNMatrix4FromMat4(hitResult.localTransform)
        return scratchNode.position.useContents { x.toFloat() to z.toFloat() }
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun createFloorDotMaterial(): SCNMaterial =
    SCNMaterial().apply {
        diffuse.contents = UIColor.whiteColor.colorWithAlphaComponent(0.92)
        lightingModelName = platform.SceneKit.SCNLightingModelConstant
    }

@OptIn(ExperimentalForeignApi::class)
private var sharedDotGeometry: SCNCylinder? = null

@OptIn(ExperimentalForeignApi::class)
private fun dotGeometry(): SCNCylinder =
    sharedDotGeometry ?: SCNCylinder.cylinderWithRadius(
        radius = FLOOR_DOT_RADIUS_M.toDouble(),
        height = FLOOR_DOT_HEIGHT_M.toDouble()
    ).also {
        sharedDotGeometry = it
    }

private fun dotLocalY(): Float = GRID_VISUAL_OFFSET_M + FLOOR_DOT_HEIGHT_M * 0.5f

@OptIn(ExperimentalForeignApi::class)
internal fun ARPlaneAnchor.readPlaneGeometry(
    centerLocal: Pair<Float, Float>?,
    visibleRadiusM: Float
): IosPlaneGeometry? {
    val refX = centerLocal?.first ?: 0f
    val refZ = centerLocal?.second ?: 0f
    val dots = PlaneGeometryBridge.generateDotPoints(
        anchor = this,
        refX = refX,
        refZ = refZ,
        maxRadiusM = visibleRadiusM
    )
    val area = PlaneGeometryBridge.polygonArea(this)
    if (area <= 0f) return null
    val fingerprint = PlaneGeometryBridge.geometrySignature(this, centerLocal, visibleRadiusM)
    return IosPlaneGeometry(
        dots = dots,
        area = area,
        fingerprint = fingerprint
    )
}

@OptIn(ExperimentalForeignApi::class)
internal fun ARPlaneAnchor.computeViewFingerprint(
    centerLocal: Pair<Float, Float>?,
    visibleRadiusM: Float
): UInt =
    PlaneGeometryBridge.geometrySignature(this, centerLocal, visibleRadiusM)

@OptIn(ExperimentalForeignApi::class)
internal fun ARPlaneAnchor.containsLocalPoint(localX: Float, localZ: Float): Boolean =
    PlaneGeometryBridge.isLocalPointInPolygon(this, localX, localZ)

@OptIn(ExperimentalForeignApi::class)
private object PlaneGeometryBridge {
    private val pointBufferX = nativeHeap.allocArray<FloatVar>(MAX_GENERATED_DOT_POINTS)
    private val pointBufferZ = nativeHeap.allocArray<FloatVar>(MAX_GENERATED_DOT_POINTS)

    fun geometrySignature(
        anchor: ARPlaneAnchor,
        centerLocal: Pair<Float, Float>?,
        visibleRadiusM: Float
    ): UInt {
        var signature = pg_geometry_signature(anchor.bridgePointer())
        if (centerLocal != null) {
            signature = signature * 31u + centerFingerprintBucket(centerLocal.first).toUInt()
            signature = signature * 31u + centerFingerprintBucket(centerLocal.second).toUInt()
            signature = signature * 31u + visibleRadiusM.toRawBits().toUInt()
        }
        return signature
    }

    fun isLocalPointInPolygon(anchor: ARPlaneAnchor, localX: Float, localZ: Float): Boolean =
        pg_local_point_in_polygon(anchor.bridgePointer(), localX, localZ)

    fun polygonArea(anchor: ARPlaneAnchor): Float =
        pg_polygon_area(anchor.bridgePointer())

    fun generateDotPoints(
        anchor: ARPlaneAnchor,
        refX: Float,
        refZ: Float,
        maxRadiusM: Float
    ): List<PlanePoint> {
        val count = pg_generate_dot_points(
            planeAnchor = anchor.bridgePointer(),
            stepM = GRID_STEP_M,
            refX = refX,
            refZ = refZ,
            maxRadiusM = maxRadiusM,
            outLocalX = pointBufferX,
            outLocalZ = pointBufferZ,
            maxPoints = MAX_GENERATED_DOT_POINTS
        )
        if (count <= 0) return emptyList()
        return List(count) { index ->
            PlanePoint(pointBufferX[index], pointBufferZ[index])
        }
    }
}

private fun centerFingerprintBucket(value: Float): Int =
    (value / CENTER_FINGERPRINT_STEP_M).roundToInt()

@OptIn(ExperimentalForeignApi::class)
private fun NSObject.bridgePointer() =
    interpretCPointer<COpaque>(objcPtr())

internal data class DotGridSyncProgress(
    val grid: SCNNode,
    val dots: List<PlanePoint>,
    val dotMaterial: SCNMaterial,
    val fingerprint: UInt,
    var nextIndex: Int = 0
) {
    val isComplete: Boolean get() = nextIndex >= dots.size
}

@OptIn(ExperimentalForeignApi::class)
internal fun syncPlaneGeometryDots(
    parentNode: SCNNode,
    geometry: IosPlaneGeometry,
    dotMaterial: SCNMaterial,
    previousFingerprint: UInt?,
    inProgress: DotGridSyncProgress?
): DotGridSyncProgress? {
    val grid = findOrCreateDotGrid(parentNode)
    grid.hidden = false

    if (inProgress != null && inProgress.grid == grid && inProgress.fingerprint == geometry.fingerprint) {
        appendDotBatch(inProgress)
        return if (inProgress.isComplete) null else inProgress
    }

    if (previousFingerprint == geometry.fingerprint && grid.childNodes.isNotEmpty()) {
        return null
    }

    val existingDots = grid.childNodes.mapNotNull { it as? SCNNode }
    if (existingDots.isEmpty() && geometry.dots.size > DOT_SYNC_BATCH_SIZE) {
        val progress = DotGridSyncProgress(
            grid = grid,
            dots = geometry.dots,
            dotMaterial = dotMaterial,
            fingerprint = geometry.fingerprint
        )
        appendDotBatch(progress)
        return if (progress.isComplete) null else progress
    }

    geometry.dots.forEachIndexed { index, point ->
        val dot = existingDots.getOrNull(index) ?: createDotNode(point.x, point.z, dotMaterial).also {
            grid.addChildNode(it)
        }
        dot.name = dotNodeName(point.x, point.z)
        dot.hidden = false
        dot.position = SCNVector3Make(point.x, dotLocalY(), point.z)
    }

    for (index in geometry.dots.size until existingDots.size) {
        existingDots[index].hidden = true
    }
    return null
}

@OptIn(ExperimentalForeignApi::class)
internal fun continueDotGridSync(progress: DotGridSyncProgress): DotGridSyncProgress? {
    appendDotBatch(progress)
    return if (progress.isComplete) null else progress
}

@OptIn(ExperimentalForeignApi::class)
private fun appendDotBatch(progress: DotGridSyncProgress) {
    val end = minOf(progress.nextIndex + DOT_SYNC_BATCH_SIZE, progress.dots.size)
    for (index in progress.nextIndex until end) {
        val point = progress.dots[index]
        val dot = createDotNode(point.x, point.z, progress.dotMaterial)
        dot.name = dotNodeName(point.x, point.z)
        progress.grid.addChildNode(dot)
    }
    progress.nextIndex = end
}

@OptIn(ExperimentalForeignApi::class)
internal fun hidePlaneDotGrid(parentNode: SCNNode) {
    findDotGrid(parentNode)?.hidden = true
}

@OptIn(ExperimentalForeignApi::class)
internal fun syncPreviewDotGrid(
    rootNode: SCNNode,
    hitResult: ARHitTestResult,
    dotMaterial: SCNMaterial
) {
    val grid = findOrCreatePreviewGrid(rootNode)
    grid.hidden = false
    grid.transform = SCNMatrix4FromMat4(hitResult.worldTransform)

    val existingDots = grid.childNodes.mapNotNull { it as? SCNNode }
    val dots = generatePreviewDots()

    dots.forEachIndexed { index, point ->
        val dot = existingDots.getOrNull(index) ?: SCNNode.nodeWithGeometry(dotGeometry()).also {
            it.geometry?.materials = listOf(dotMaterial)
            grid.addChildNode(it)
        }
        dot.hidden = false
        dot.position = SCNVector3Make(point.x, dotLocalY(), point.z)
    }

    for (index in dots.size until existingDots.size) {
        existingDots[index].hidden = true
    }
}

internal fun hidePreviewDotGrid(rootNode: SCNNode) {
    findPreviewGrid(rootNode)?.hidden = true
}

internal fun planeDotGridIsHidden(parentNode: SCNNode): Boolean {
    val grid = findDotGrid(parentNode) ?: return true
    return grid.hidden
}

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

@OptIn(ExperimentalForeignApi::class)
private fun createDotNode(
    localX: Float,
    localZ: Float,
    dotMaterial: SCNMaterial
): SCNNode {
    val dot = SCNNode.nodeWithGeometry(dotGeometry())
    dot.geometry?.materials = listOf(dotMaterial)
    dot.position = SCNVector3Make(localX, dotLocalY(), localZ)
    return dot
}

private fun dotNodeName(localX: Float, localZ: Float): String =
    "dot_${localX.toRawBits()}_${localZ.toRawBits()}"

private fun generatePreviewDots(): List<PlanePoint> {
    val dots = mutableListOf<PlanePoint>()
    val radiusSq = FLOOR_DOT_RADIUS_M * FLOOR_DOT_RADIUS_M
    var x = -FLOOR_DOT_RADIUS_M
    while (x <= FLOOR_DOT_RADIUS_M + GRID_STEP_M * 0.5f) {
        var z = -FLOOR_DOT_RADIUS_M
        while (z <= FLOOR_DOT_RADIUS_M + GRID_STEP_M * 0.5f) {
            if (x * x + z * z <= radiusSq) {
                dots += PlanePoint(x, z)
            }
            z += GRID_STEP_M
        }
        x += GRID_STEP_M
    }
    return dots
}

@OptIn(ExperimentalForeignApi::class)
internal fun List<*>.firstHorizontalFloorHitResult(): ARHitTestResult? {
    for (item in this) {
        val result = item as? ARHitTestResult ?: continue
        val anchor = result.anchor as? ARPlaneAnchor ?: continue
        if (anchor.isHorizontalTracking()) return result
    }
    return null
}

internal fun List<*>.firstEstimatedHorizontalFloorHitResult(): ARHitTestResult? {
    for (item in this) {
        val result = item as? ARHitTestResult ?: continue
        return result
    }
    return null
}

internal typealias PlaneFingerprints = MutableMap<NSUUID, UInt>
internal typealias PlaneAreas = MutableMap<NSUUID, Float>
internal typealias PlaneDotCounts = MutableMap<NSUUID, Int>

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

internal const val VISIBLE_DOT_RADIUS_M = 2.5f
internal const val FOCUS_GRACE_FRAMES = 12
internal const val GRID_STEP_M = 0.14f
internal const val GRID_VISUAL_OFFSET_M = 0.003f
internal const val FLOOR_DOT_RADIUS_M = 0.013f
private const val FLOOR_DOT_HEIGHT_M = 0.001f
internal const val MIN_FLOOR_AREA_M2 = 0.15f
internal const val PLANE_GRID_NODE_NAME = "plane-grid"
private const val PREVIEW_GRID_NODE_NAME = "plane-preview-grid"
private const val CENTER_FINGERPRINT_STEP_M = 0.3f
private const val MAX_GENERATED_DOT_POINTS = 2048
private const val DOT_SYNC_BATCH_SIZE = 128
