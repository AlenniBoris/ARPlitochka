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
import kotlin.math.sqrt

private const val POINT_RADIUS_M = 0.016f
private const val POINT_HEIGHT_M = 0.002f
private const val POINT_VISUAL_OFFSET_M = 0.008f
private const val LINE_WIDTH_M = 0.012f
private const val LINE_HEIGHT_M = 0.002f
private const val LINE_VISUAL_OFFSET_M = 0.003f
private const val PREVIEW_LINE_WIDTH_M = 0.016f
private const val PREVIEW_LINE_HEIGHT_M = 0.002f
private const val PREVIEW_LINE_VISUAL_OFFSET_M = 0.005f
private const val MIN_LINE_LENGTH_M = 0.001f

@OptIn(ExperimentalForeignApi::class)
internal class IosArContourRenderer {
    private var rootNode: SCNNode? = null
    private val pointNodes = mutableMapOf<String, SCNNode>()
    private var linesContainer: SCNNode? = null

    private val pointMaterial = createContourMaterial(red = 0.0, green = 0.9, blue = 0.46)
    private val snapPointMaterial = createContourMaterial(red = 0.41, green = 0.94, blue = 0.68)
    private val lineMaterial = createContourMaterial(red = 0.13, green = 0.59, blue = 0.95)
    private val previewLineMaterial = createContourMaterial(red = 0.0, green = 0.64, blue = 1.0)

    fun attach(sceneRoot: SCNNode) {
        if (rootNode != null) return
        rootNode = SCNNode().also { parent ->
            linesContainer = SCNNode().also { parent.addChildNode(it) }
            sceneRoot.addChildNode(parent)
        }
    }

    fun detach() {
        pointNodes.values.forEach { it.removeFromParentNode() }
        pointNodes.clear()
        linesContainer?.removeFromParentNode()
        linesContainer = null
        rootNode?.removeFromParentNode()
        rootNode = null
    }

    fun sync(state: FloorContourUiState) {
        if (rootNode == null) return
        val floorY = state.placedPoints.firstOrNull()?.position?.yMeters

        syncPointNodes(state, floorY)
        syncLines(state, floorY)
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
                    )
                    rootNode?.addChildNode(this)
                }
            }
            node.geometry?.firstMaterial = if (state.snappedPointIndex == index) {
                snapPointMaterial
            } else {
                pointMaterial
            }
            node.position = SCNVector3Make(placed.position.xMeters, y, placed.position.zMeters)
        }
    }

    private fun syncLines(state: FloorContourUiState, floorY: Float?) {
        val container = linesContainer ?: return
        container.childNodes.mapNotNull { it as? SCNNode }.forEach { it.removeFromParentNode() }

        val points = state.placedPoints.map { it.position }
        if (state.showContourLines) {
            for (index in 0 until points.lastIndex) {
                addSegment(container, points[index], points[index + 1], floorY, lineMaterial, isPreview = false)
            }
            if (state.isPolygonClosed) {
                addSegment(container, points.last(), points.first(), floorY, lineMaterial, isPreview = false)
            }
        }

        val currentHitPoint = state.currentHitPoint
        if (state.showPreviewLine && currentHitPoint != null) {
            addSegment(
                parent = container,
                start = points.last(),
                end = currentHitPoint,
                floorY = floorY,
                material = previewLineMaterial,
                isPreview = true
            )
        }
    }

    private fun addSegment(
        parent: SCNNode,
        start: ArPoint3D,
        end: ArPoint3D,
        floorY: Float?,
        material: SCNMaterial,
        isPreview: Boolean
    ) {
        val dx = end.xMeters - start.xMeters
        val dz = end.zMeters - start.zMeters
        val length = sqrt(dx * dx + dz * dz)
        if (length < MIN_LINE_LENGTH_M) return

        val yBase = floorY ?: start.yMeters
        val offset = if (isPreview) PREVIEW_LINE_VISUAL_OFFSET_M else LINE_VISUAL_OFFSET_M
        val width = if (isPreview) PREVIEW_LINE_WIDTH_M else LINE_WIDTH_M
        val height = if (isPreview) PREVIEW_LINE_HEIGHT_M else LINE_HEIGHT_M

        val midX = (start.xMeters + end.xMeters) * 0.5f
        val midZ = (start.zMeters + end.zMeters) * 0.5f

        val node = SCNNode()
        node.geometry = SCNBox.boxWithWidth(
            width = width.toDouble(),
            height = height.toDouble(),
            length = length.toDouble(),
            chamferRadius = 0.0
        ).apply {
            this.materials = listOf(material)
        }
        node.position = SCNVector3Make(midX, yBase + offset, midZ)
        node.eulerAngles = SCNVector3Make(0f, atan2(dx, dz), 0f)
        parent.addChildNode(node)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun createContourMaterial(red: Double, green: Double, blue: Double): SCNMaterial =
    SCNMaterial().apply {
        diffuse.contents = UIColor.colorWithRed(red, green, blue, alpha = 1.0)
        lightingModelName = platform.SceneKit.SCNLightingModelConstant
    }
