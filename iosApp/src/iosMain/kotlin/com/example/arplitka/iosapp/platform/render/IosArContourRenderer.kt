package com.example.arplitka.iosapp.platform.render

import com.example.arplitka.iosapp.bridge.pg_create_contour_lines_geometry
import com.example.arplitka.shared.ar.contracts.model.ArPoint3D
import com.example.arplitka.shared.ar.domain.model.FloorContourUiState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.SceneKit.SCNCylinder
import platform.SceneKit.SCNMaterial
import platform.SceneKit.SCNNode
import platform.SceneKit.SCNVector3Make
import platform.UIKit.UIColor
import kotlin.math.roundToInt

private const val POINT_RADIUS_M = 0.016f
private const val POINT_HEIGHT_M = 0.002f
private const val POINT_VISUAL_OFFSET_M = 0.008f
private const val LINE_WIDTH_M = 0.012f
private const val LINE_VISUAL_OFFSET_M = 0.003f
private const val LOD_POINT_COUNT_THRESHOLD = 30
private const val LOD_POINT_RADIUS_SCALE = 0.85f
private const val LOD_LINE_WIDTH_SCALE = 0.80f
private const val POSITION_QUANT_M = 0.01f

@OptIn(ExperimentalForeignApi::class)
internal class IosArContourRenderer {
    private var rootNode: SCNNode? = null
    private val pointNodes = mutableMapOf<String, SCNNode>()
    private var batchedLinesNode: SCNNode? = null
    private var lastContourStateKey: Int = Int.MIN_VALUE
    private var lastLineBatchKey: Int = Int.MIN_VALUE
    private val lastPointPositionKeys = mutableMapOf<String, Int>()

    private val pointMaterial = createContourMaterial(red = 0.0, green = 0.9, blue = 0.46)
    private val snapPointMaterial = createContourMaterial(red = 0.41, green = 0.94, blue = 0.68)
    private val lineMaterial = createContourMaterial(red = 0.13, green = 0.59, blue = 0.95)

    fun attach(sceneRoot: SCNNode) {
        if (rootNode != null) return
        rootNode = SCNNode().also { sceneRoot.addChildNode(it) }
    }

    fun detach() {
        pointNodes.values.forEach { it.removeFromParentNode() }
        pointNodes.clear()
        batchedLinesNode?.removeFromParentNode()
        batchedLinesNode = null
        rootNode?.removeFromParentNode()
        rootNode = null
        lastContourStateKey = Int.MIN_VALUE
        lastLineBatchKey = Int.MIN_VALUE
        lastPointPositionKeys.clear()
    }

    fun syncIfChanged(state: FloorContourUiState): Boolean {
        val structureKey = contourStructureKey(state)
        if (structureKey == lastContourStateKey) return false
        lastContourStateKey = structureKey
        syncContour(state)
        return true
    }

    private fun syncContour(state: FloorContourUiState) {
        if (rootNode == null) return
        val floorY = state.placedPoints.firstOrNull()?.position?.yMeters
        val lodActive = state.placedPoints.size >= LOD_POINT_COUNT_THRESHOLD
        syncPointNodes(state, floorY, lodActive)
        syncBatchedLines(state, floorY, lodActive)
    }

    private fun syncPointNodes(
        state: FloorContourUiState,
        floorY: Float?,
        lodActive: Boolean
    ) {
        val activeIds = if (state.showContourPoints) {
            state.placedPoints.map { it.id }.toSet()
        } else {
            emptySet()
        }
        pointNodes.keys.filterNot { it in activeIds }.forEach { id ->
            pointNodes.remove(id)?.removeFromParentNode()
            lastPointPositionKeys.remove(id)
        }
        if (!state.showContourPoints) return

        val pointScale = if (lodActive) LOD_POINT_RADIUS_SCALE else 1f
        state.placedPoints.forEachIndexed { index, placed ->
            val y = (floorY ?: placed.position.yMeters) + POINT_VISUAL_OFFSET_M
            val positionKey = positionKey(placed.position.xMeters, y, placed.position.zMeters)
            val node = pointNodes.getOrPut(placed.id) {
                SCNNode().apply {
                    geometry = SCNCylinder.cylinderWithRadius(
                        radius = POINT_RADIUS_M.toDouble(),
                        height = POINT_HEIGHT_M.toDouble()
                    ).apply {
                        materials = listOf(pointMaterial)
                    }
                    rootNode?.addChildNode(this)
                }
            }
            node.scale = SCNVector3Make(pointScale, 1f, pointScale)
            if (lastPointPositionKeys[placed.id] != positionKey) {
                lastPointPositionKeys[placed.id] = positionKey
                node.position = SCNVector3Make(placed.position.xMeters, y, placed.position.zMeters)
            }
            val targetMaterial = if (state.snappedPointIndex == index) snapPointMaterial else pointMaterial
            if (node.geometry?.firstMaterial != targetMaterial) {
                node.geometry?.firstMaterial = targetMaterial
            }
        }
    }

    private fun syncBatchedLines(
        state: FloorContourUiState,
        floorY: Float?,
        lodActive: Boolean
    ) {
        val points = state.placedPoints.map { it.position }
        val segmentCount = when {
            !state.showContourLines -> 0
            state.isPolygonClosed && points.size >= 3 -> points.size
            points.size >= 2 -> points.lastIndex
            else -> 0
        }

        if (segmentCount == 0) {
            batchedLinesNode?.hidden = true
            return
        }

        val lineBatchKey = lineBatchKey(points, segmentCount, floorY, state.isPolygonClosed, lodActive)
        if (lineBatchKey == lastLineBatchKey) return
        lastLineBatchKey = lineBatchKey

        val halfWidth = (if (lodActive) LINE_WIDTH_M * LOD_LINE_WIDTH_SCALE else LINE_WIDTH_M) * 0.5f
        val yBase = (floorY ?: points.first().yMeters) + LINE_VISUAL_OFFSET_M
        val segmentBuffer = FloatArray(segmentCount * 4)
        for (index in 0 until segmentCount) {
            val start = points[index]
            val end = if (index < points.lastIndex) points[index + 1] else points.first()
            val offset = index * 4
            segmentBuffer[offset] = start.xMeters
            segmentBuffer[offset + 1] = start.zMeters
            segmentBuffer[offset + 2] = end.xMeters
            segmentBuffer[offset + 3] = end.zMeters
        }

        val geometry = segmentBuffer.usePinned { pinned ->
            pg_create_contour_lines_geometry(
                segmentCount = segmentCount,
                segmentPairsXZ = pinned.addressOf(0),
                yM = yBase,
                halfWidthM = halfWidth
            )
        } ?: run {
            batchedLinesNode?.hidden = true
            return
        }

        val node = batchedLinesNode ?: SCNNode().also { created ->
            batchedLinesNode = created
            rootNode?.addChildNode(created)
        }
        node.geometry = geometry
        node.geometry?.firstMaterial = lineMaterial
        node.hidden = false
    }

    private fun contourStructureKey(state: FloorContourUiState): Int {
        var key = state.placedPoints.size * 31
        state.placedPoints.forEach { point ->
            key = key * 31 + (point.position.xMeters * 100f).roundToInt()
            key = key * 31 + (point.position.zMeters * 100f).roundToInt()
            key = key * 31 + (point.position.yMeters * 100f).roundToInt()
        }
        key = key * 31 + (state.snappedPointIndex ?: -1)
        key = key * 31 + if (state.isPolygonClosed) 1 else 0
        key = key * 31 + if (state.showContourPoints) 1 else 0
        key = key * 31 + if (state.showContourLines) 1 else 0
        key = key * 31 + if (state.placedPoints.size >= LOD_POINT_COUNT_THRESHOLD) 1 else 0
        return key
    }

    private fun lineBatchKey(
        points: List<ArPoint3D>,
        segmentCount: Int,
        floorY: Float?,
        isPolygonClosed: Boolean,
        lodActive: Boolean
    ): Int {
        var key = segmentCount * 31
        key = key * 31 + if (isPolygonClosed) 1 else 0
        key = key * 31 + if (lodActive) 1 else 0
        key = key * 31 + ((floorY ?: 0f) / POSITION_QUANT_M).roundToInt()
        for (index in 0 until segmentCount) {
            val start = points[index]
            val end = if (index < points.lastIndex) points[index + 1] else points.first()
            key = key * 31 + quant(start.xMeters)
            key = key * 31 + quant(start.zMeters)
            key = key * 31 + quant(end.xMeters)
            key = key * 31 + quant(end.zMeters)
        }
        return key
    }

    private fun positionKey(x: Float, y: Float, z: Float): Int =
        listOf(quant(x), quant(y), quant(z)).hashCode()

    private fun quant(value: Float): Int = (value / POSITION_QUANT_M).roundToInt()
}

@OptIn(ExperimentalForeignApi::class)
private fun createContourMaterial(
    red: Double,
    green: Double,
    blue: Double,
    alpha: Double = 1.0
): SCNMaterial =
    SCNMaterial().apply {
        diffuse.contents = UIColor.colorWithRed(red, green, blue, alpha = alpha)
        lightingModelName = platform.SceneKit.SCNLightingModelConstant
    }
