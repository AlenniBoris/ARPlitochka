package com.example.arplitka.iosapp

import com.example.arplitka.shared.ar.contracts.model.ArPoint3D
import com.example.arplitka.shared.ar.domain.model.FloorContourUiState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.SceneKit.SCNBox
import platform.SceneKit.SCNCylinder
import platform.SceneKit.SCNMaterial
import platform.SceneKit.SCNNode
import platform.SceneKit.SCNVector3Make
import platform.UIKit.UIColor
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val POINT_RADIUS_M = 0.016f
private const val POINT_HEIGHT_M = 0.002f
private const val POINT_VISUAL_OFFSET_M = 0.008f
private const val LINE_WIDTH_M = 0.012f
private const val LINE_HEIGHT_M = 0.002f
private const val LINE_VISUAL_OFFSET_M = 0.003f
private const val MIN_LINE_LENGTH_M = 0.001f
private const val LINE_GEOMETRY_QUANT_M = 0.02f

@OptIn(ExperimentalForeignApi::class)
internal class IosArContourRenderer {
    private var rootNode: SCNNode? = null
    private val pointNodes = mutableMapOf<String, SCNNode>()
    private val fixedLineNodes = mutableListOf<SCNNode>()
    private var previewLineNode: SCNNode? = null
    private var lastFixedSegmentCount: Int = 0
    private var lastPolygonClosed: Boolean = false
    private var lastContourStateKey: Int = Int.MIN_VALUE
    private val lastLineGeometryKeys = mutableListOf<Int>()

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
        fixedLineNodes.forEach { it.removeFromParentNode() }
        fixedLineNodes.clear()
        previewLineNode?.removeFromParentNode()
        previewLineNode = null
        rootNode?.removeFromParentNode()
        rootNode = null
        lastFixedSegmentCount = 0
        lastPolygonClosed = false
        lastContourStateKey = Int.MIN_VALUE
        lastLineGeometryKeys.clear()
    }

    fun syncIfChanged(state: FloorContourUiState): Boolean {
        val key = contourStateKey(state)
        if (key == lastContourStateKey) return false
        lastContourStateKey = key
        sync(state)
        return true
    }

    fun sync(state: FloorContourUiState) {
        if (rootNode == null) return
        val floorY = state.placedPoints.firstOrNull()?.position?.yMeters
        syncPointNodes(state, floorY)
        syncFixedLines(state, floorY)
        hidePreviewLine()
    }

    private fun syncPointNodes(state: FloorContourUiState, floorY: Float?) {
        val activeIds = if (state.showContourPoints) {
            state.placedPoints.map { it.id }.toSet()
        } else {
            emptySet()
        }
        pointNodes.keys.filterNot { it in activeIds }.forEach { id ->
            pointNodes.remove(id)?.removeFromParentNode()
        }
        if (!state.showContourPoints) return

        state.placedPoints.forEachIndexed { index, placed ->
            val y = (floorY ?: placed.position.yMeters) + POINT_VISUAL_OFFSET_M
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
            val targetMaterial = if (state.snappedPointIndex == index) snapPointMaterial else pointMaterial
            if (node.geometry?.firstMaterial != targetMaterial) {
                node.geometry?.firstMaterial = targetMaterial
            }
            node.position = SCNVector3Make(placed.position.xMeters, y, placed.position.zMeters)
        }
    }

    private fun syncFixedLines(state: FloorContourUiState, floorY: Float?) {
        val points = state.placedPoints.map { it.position }
        val segmentCount = when {
            !state.showContourLines -> 0
            state.isPolygonClosed && points.size >= 3 -> points.size
            points.size >= 2 -> points.lastIndex
            else -> 0
        }

        if (segmentCount != lastFixedSegmentCount || state.isPolygonClosed != lastPolygonClosed) {
            lastFixedSegmentCount = segmentCount
            lastPolygonClosed = state.isPolygonClosed
            while (fixedLineNodes.size > segmentCount) {
                fixedLineNodes.removeAt(fixedLineNodes.lastIndex).removeFromParentNode()
            }
            while (fixedLineNodes.size < segmentCount) {
                fixedLineNodes += createLineNode().also { rootNode?.addChildNode(it) }
            }
            lastLineGeometryKeys.clear()
        }
        if (segmentCount == 0) return

        for (index in 0 until segmentCount) {
            val start = points[index]
            val end = if (index < points.lastIndex) points[index + 1] else points.first()
            updateLineNode(fixedLineNodes[index], index, start, end, floorY)
        }
    }

    private fun hidePreviewLine() {
        previewLineNode?.hidden = true
    }

    private fun createLineNode(): SCNNode =
        SCNNode().apply {
            geometry = SCNBox.boxWithWidth(
                width = LINE_WIDTH_M.toDouble(),
                height = LINE_HEIGHT_M.toDouble(),
                length = 1.0,
                chamferRadius = 0.0
            ).apply {
                materials = listOf(lineMaterial)
            }
        }

    private fun updateLineNode(
        node: SCNNode,
        index: Int,
        start: ArPoint3D,
        end: ArPoint3D,
        floorY: Float?
    ) {
        val dx = end.xMeters - start.xMeters
        val dz = end.zMeters - start.zMeters
        val length = sqrt(dx * dx + dz * dz)
        if (length < MIN_LINE_LENGTH_M) {
            node.hidden = true
            return
        }
        node.hidden = false

        val yBase = floorY ?: start.yMeters
        val midX = (start.xMeters + end.xMeters) * 0.5f
        val midZ = (start.zMeters + end.zMeters) * 0.5f
        val geometryKey = lineGeometryKey(start, end, yBase, length)

        if (lastLineGeometryKeys.getOrNull(index) != geometryKey) {
            while (lastLineGeometryKeys.size <= index) {
                lastLineGeometryKeys.add(0)
            }
            lastLineGeometryKeys[index] = geometryKey
            if (node.geometry == null) {
                node.geometry = SCNBox.boxWithWidth(
                    width = LINE_WIDTH_M.toDouble(),
                    height = LINE_HEIGHT_M.toDouble(),
                    length = 1.0,
                    chamferRadius = 0.0
                ).apply {
                    materials = listOf(lineMaterial)
                }
            }
        }

        node.scale = SCNVector3Make(1f, 1f, length)
        node.position = SCNVector3Make(midX, yBase + LINE_VISUAL_OFFSET_M, midZ)
        node.eulerAngles = SCNVector3Make(0f, atan2(dx, dz), 0f)
    }

    private fun contourStateKey(state: FloorContourUiState): Int {
        var key = state.placedPoints.size * 31
        state.placedPoints.forEach { point ->
            key = key * 31 + (point.position.xMeters * 50f).roundToInt()
            key = key * 31 + (point.position.zMeters * 50f).roundToInt()
            key = key * 31 + (point.position.yMeters * 50f).roundToInt()
        }
        key = key * 31 + (state.snappedPointIndex ?: -1)
        key = key * 31 + if (state.isPolygonClosed) 1 else 0
        key = key * 31 + if (state.showContourPoints) 1 else 0
        key = key * 31 + if (state.showContourLines) 1 else 0
        return key
    }

    private fun lineGeometryKey(
        start: ArPoint3D,
        end: ArPoint3D,
        yBase: Float,
        length: Float
    ): Int {
        val sx = (start.xMeters / LINE_GEOMETRY_QUANT_M).roundToInt()
        val sz = (start.zMeters / LINE_GEOMETRY_QUANT_M).roundToInt()
        val ex = (end.xMeters / LINE_GEOMETRY_QUANT_M).roundToInt()
        val ez = (end.zMeters / LINE_GEOMETRY_QUANT_M).roundToInt()
        val ly = (length / LINE_GEOMETRY_QUANT_M).roundToInt()
        val y = (yBase / LINE_GEOMETRY_QUANT_M).roundToInt()
        return listOf(sx, sz, ex, ez, ly, y).hashCode()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun createContourMaterial(red: Double, green: Double, blue: Double): SCNMaterial =
    SCNMaterial().apply {
        diffuse.contents = UIColor.colorWithRed(red, green, blue, alpha = 1.0)
        lightingModelName = platform.SceneKit.SCNLightingModelConstant
    }
