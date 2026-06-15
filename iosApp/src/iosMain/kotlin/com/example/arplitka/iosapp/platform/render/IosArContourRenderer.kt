package com.example.arplitka.iosapp.platform.render

import com.example.arplitka.iosapp.bridge.pg_create_contour_distance_label_image
import com.example.arplitka.iosapp.bridge.pg_create_contour_fill_geometry
import com.example.arplitka.iosapp.bridge.pg_create_contour_lines_geometry
import com.example.arplitka.iosapp.bridge.pg_create_tile_section_pattern_image
import com.example.arplitka.iosapp.bridge.pg_load_tile_texture_image
import com.example.arplitka.shared.ar.domain.geometry.CONTOUR_LABEL_HEIGHT_M
import com.example.arplitka.shared.ar.domain.geometry.CONTOUR_LABEL_WIDTH_M
import com.example.arplitka.shared.ar.domain.geometry.buildContourSegmentLabels
import com.example.arplitka.shared.ar.domain.geometry.contourDistanceLabelBatchKey
import com.example.arplitka.shared.ar.domain.geometry.AlignedSectionGeometry
import com.example.arplitka.shared.ar.domain.geometry.buildAlignedSectionGeometry
import com.example.arplitka.shared.ar.domain.geometry.formatContourDistanceMeters
import com.example.arplitka.shared.ar.contracts.model.ArPoint3D
import com.example.arplitka.shared.ar.domain.model.FloorContourUiState
import com.example.arplitka.shared.ar.domain.model.TextureRotation
import com.example.arplitka.shared.ar.domain.model.TileType
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.SceneKit.SCNCylinder
import platform.SceneKit.SCNMaterial
import platform.SceneKit.SCNNode
import platform.SceneKit.SCNPlane
import platform.SceneKit.SCNVector3Make
import platform.UIKit.UIColor
import kotlin.math.PI
import kotlin.math.roundToInt

private const val POINT_RADIUS_M = 0.016f
private const val POINT_HEIGHT_M = 0.002f
private const val POINT_VISUAL_OFFSET_M = 0.008f
private const val LINE_WIDTH_M = 0.012f
private const val LINE_VISUAL_OFFSET_M = 0.003f
private const val FILL_VISUAL_OFFSET_M = 0.004f
private const val LOD_POINT_COUNT_THRESHOLD = 30
private const val LOD_POINT_RADIUS_SCALE = 0.85f
private const val LOD_LINE_WIDTH_SCALE = 0.80f
private const val POSITION_QUANT_M = 0.01f

@OptIn(ExperimentalForeignApi::class)
internal class IosArContourRenderer {
    private var rootNode: SCNNode? = null
    private val pointNodes = mutableMapOf<String, SCNNode>()
    private var batchedLinesNode: SCNNode? = null
    private var sectionFillNode: SCNNode? = null
    private val distanceLabelNodes = mutableMapOf<String, SCNNode>()
    private val lastDistanceLabelTexts = mutableMapOf<String, String>()
    private var lastContourStateKey: Int = Int.MIN_VALUE
    private var lastLineBatchKey: Int = Int.MIN_VALUE
    private var lastFillBatchKey: Int = Int.MIN_VALUE
    private var lastDistanceLabelBatchKey: Int = Int.MIN_VALUE
    private val lastPointPositionKeys = mutableMapOf<String, Int>()

    private val pointMaterial = createContourMaterial(red = 0.0, green = 0.9, blue = 0.46)
    private val snapPointMaterial = createContourMaterial(red = 0.41, green = 0.94, blue = 0.68)
    private val lineMaterial = createContourMaterial(red = 0.13, green = 0.59, blue = 0.95)
    private val fillMaterial = createFillMaterial(
        red = 0.18,
        green = 0.62,
        blue = 0.98,
        alpha = 0.58
    )
    private val tileMaterialCache = mutableMapOf<TileMaterialKey, SCNMaterial>()

    fun attach(sceneRoot: SCNNode) {
        if (rootNode != null) return
        rootNode = SCNNode().also { sceneRoot.addChildNode(it) }
    }

    fun detach() {
        pointNodes.values.forEach { it.removeFromParentNode() }
        pointNodes.clear()
        batchedLinesNode?.removeFromParentNode()
        batchedLinesNode = null
        sectionFillNode?.removeFromParentNode()
        sectionFillNode = null
        distanceLabelNodes.values.forEach { it.removeFromParentNode() }
        distanceLabelNodes.clear()
        lastDistanceLabelTexts.clear()
        rootNode?.removeFromParentNode()
        rootNode = null
        lastContourStateKey = Int.MIN_VALUE
        lastLineBatchKey = Int.MIN_VALUE
        lastFillBatchKey = Int.MIN_VALUE
        lastDistanceLabelBatchKey = Int.MIN_VALUE
        lastPointPositionKeys.clear()
        tileMaterialCache.clear()
    }

    fun syncIfChanged(state: FloorContourUiState): Boolean {
        val structureKey = contourStructureKey(state)
        val structureChanged = structureKey != lastContourStateKey
        if (structureChanged) {
            lastContourStateKey = structureKey
        }
        syncContour(state)
        return structureChanged
    }

    private fun syncContour(state: FloorContourUiState) {
        if (rootNode == null) return
        val floorY = state.placedPoints.firstOrNull()?.position?.yMeters
        val lodActive = state.placedPoints.size >= LOD_POINT_COUNT_THRESHOLD
        syncSectionFill(state, floorY)
        syncBatchedLines(state, floorY, lodActive)
        syncDistanceLabels(state, floorY)
        syncPointNodes(state, floorY, lodActive)
    }

    private fun syncSectionFill(
        state: FloorContourUiState,
        floorY: Float?
    ) {
        if (!state.showSectionFill || state.placedPoints.size < 3) {
            sectionFillNode?.hidden = true
            lastFillBatchKey = Int.MIN_VALUE
            return
        }

        val points = state.placedPoints.map { it.position }
        val aligned = buildAlignedSectionGeometry(points)
        val fillBatchKey = fillBatchKey(
            points = points,
            floorY = floorY,
            isFinalized = state.isFinalized,
            isTileVisible = state.isTileVisible,
            selectedTileType = state.selectedTileType,
            textureRotation = state.textureRotation,
            aligned = aligned
        )
        if (fillBatchKey == lastFillBatchKey) {
            sectionFillNode?.hidden = false
            return
        }
        lastFillBatchKey = fillBatchKey

        val yBase = (floorY ?: points.first().yMeters) + FILL_VISUAL_OFFSET_M
        val pointBuffer = FloatArray(aligned.localPoints.size * 2)
        aligned.localPoints.forEachIndexed { index, local ->
            val offset = index * 2
            pointBuffer[offset] = local.xMeters
            pointBuffer[offset + 1] = local.yMeters
        }

        val geometry = pointBuffer.usePinned { pinned ->
            pg_create_contour_fill_geometry(
                pointCount = aligned.localPoints.size,
                pointsXZ = pinned.addressOf(0),
                yM = 0f
            )
        } ?: run {
            sectionFillNode?.hidden = true
            return
        }

        val node = sectionFillNode ?: SCNNode().also { created ->
            sectionFillNode = created
            rootNode?.addChildNode(created)
        }
        geometry.materials = listOf(resolveFillMaterial(state, aligned))
        node.geometry = geometry
        node.position = SCNVector3Make(aligned.centroidX, yBase, aligned.centroidZ)
        node.eulerAngles = SCNVector3Make(
            0f,
            eulerDegreesToRadians(aligned.rotationYDegrees),
            0f
        )
        node.renderingOrder = 5
        node.hidden = false
        rootNode?.addChildNode(node)
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
            node.hidden = false
            rootNode?.addChildNode(node)
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
        if (lineBatchKey == lastLineBatchKey) {
            batchedLinesNode?.hidden = false
            return
        }
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
        node.geometry?.materials = listOf(lineMaterial)
        node.renderingOrder = 6
        node.hidden = false
    }

    private fun syncDistanceLabels(
        state: FloorContourUiState,
        floorY: Float?
    ) {
        val segments = buildContourSegmentLabels(state, floorY)
        if (!state.showContourLines || segments.isEmpty()) {
            distanceLabelNodes.values.forEach { it.hidden = true }
            lastDistanceLabelBatchKey = Int.MIN_VALUE
            return
        }

        val activeKeys = segments.map { it.key }.toSet()
        distanceLabelNodes.keys.filterNot { it in activeKeys }.forEach { key ->
            distanceLabelNodes.remove(key)?.removeFromParentNode()
            lastDistanceLabelTexts.remove(key)
        }

        val labelBatchKey = contourDistanceLabelBatchKey(segments, floorY, state.showContourLines)
        val geometryChanged = labelBatchKey != lastDistanceLabelBatchKey
        if (geometryChanged) {
            lastDistanceLabelBatchKey = labelBatchKey
        }

        segments.forEach { segment ->
            val labelText = segment.distanceMeters.formatContourDistanceMeters()
            val node = distanceLabelNodes.getOrPut(segment.key) {
                SCNNode().also { created ->
                    rootNode?.addChildNode(created)
                }
            }
            if (geometryChanged || lastDistanceLabelTexts[segment.key] != labelText) {
                lastDistanceLabelTexts[segment.key] = labelText
                node.geometry = createDistanceLabelPlane(labelText)
            }
            node.position = SCNVector3Make(segment.midpointX, segment.midpointY, segment.midpointZ)
            node.eulerAngles = SCNVector3Make(
                eulerDegreesToRadians(-90f),
                eulerDegreesToRadians(segment.rotationYDegrees),
                0f
            )
            node.renderingOrder = 8
            node.hidden = false
            rootNode?.addChildNode(node)
        }
    }

    private fun createDistanceLabelPlane(labelText: String): SCNPlane {
        val image = pg_create_contour_distance_label_image(labelText)
        val plane = SCNPlane.planeWithWidth(
            width = CONTOUR_LABEL_WIDTH_M.toDouble(),
            height = CONTOUR_LABEL_HEIGHT_M.toDouble()
        )
        val material = createLabelMaterial()
        material.diffuse.contents = image
        plane.materials = listOf(material)
        return plane
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
        key = key * 31 + if (state.showSectionFill) 1 else 0
        key = key * 31 + if (state.isFinalized) 1 else 0
        key = key * 31 + if (state.isTileVisible) 1 else 0
        key = key * 31 + state.selectedTileType.ordinal
        key = key * 31 + state.textureRotation.ordinal
        key = key * 31 + if (state.placedPoints.size >= LOD_POINT_COUNT_THRESHOLD) 1 else 0
        return key
    }

    private fun resolveFillMaterial(
        state: FloorContourUiState,
        aligned: AlignedSectionGeometry
    ): SCNMaterial {
        if (!state.isTileVisible) return fillMaterial
        val cacheKey = TileMaterialKey(
            tileType = state.selectedTileType,
            textureRotation = state.textureRotation,
            widthQuant = quant(aligned.boundsWidthM),
            heightQuant = quant(aligned.boundsHeightM)
        )
        return tileMaterialCache.getOrPut(cacheKey) {
            createTileMaterial(
                resourceName = state.selectedTileType.resourceName,
                widthMeters = aligned.boundsWidthM,
                heightMeters = aligned.boundsHeightM,
                rotationDegrees = state.textureRotation.degrees.toFloat()
            )
        }
    }

    private fun fillBatchKey(
        points: List<ArPoint3D>,
        floorY: Float?,
        isFinalized: Boolean,
        isTileVisible: Boolean,
        selectedTileType: TileType,
        textureRotation: TextureRotation,
        aligned: AlignedSectionGeometry
    ): Int {
        var key = points.size * 31
        key = key * 31 + if (isFinalized) 1 else 0
        key = key * 31 + if (isTileVisible) 1 else 0
        key = key * 31 + selectedTileType.ordinal
        key = key * 31 + textureRotation.ordinal
        key = key * 31 + quant(aligned.boundsWidthM)
        key = key * 31 + quant(aligned.boundsHeightM)
        key = key * 31 + aligned.rotationYDegrees.roundToInt()
        key = key * 31 + ((floorY ?: 0f) / POSITION_QUANT_M).roundToInt()
        points.forEach { point ->
            key = key * 31 + quant(point.xMeters)
            key = key * 31 + quant(point.zMeters)
        }
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

private data class TileMaterialKey(
    val tileType: TileType,
    val textureRotation: TextureRotation,
    val widthQuant: Int,
    val heightQuant: Int
)

private fun eulerDegreesToRadians(degrees: Float): Float = degrees * PI.toFloat() / 180f

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

@OptIn(ExperimentalForeignApi::class)
private fun createLabelMaterial(): SCNMaterial =
    SCNMaterial().apply {
        lightingModelName = platform.SceneKit.SCNLightingModelConstant
        doubleSided = true
        readsFromDepthBuffer = true
        writesToDepthBuffer = false
    }

@OptIn(ExperimentalForeignApi::class)
private fun createTileMaterial(
    resourceName: String,
    widthMeters: Float,
    heightMeters: Float,
    rotationDegrees: Float
): SCNMaterial {
    val patternImage = pg_create_tile_section_pattern_image(
        resourceName = resourceName,
        widthMeters = widthMeters,
        heightMeters = heightMeters,
        rotationDegrees = rotationDegrees
    )
    return SCNMaterial().apply {
        diffuse.contents = patternImage ?: pg_load_tile_texture_image(resourceName, rotationDegrees)
        lightingModelName = platform.SceneKit.SCNLightingModelConstant
        doubleSided = true
        readsFromDepthBuffer = true
        writesToDepthBuffer = false
        diffuse.wrapS = platform.SceneKit.SCNWrapModeClamp
        diffuse.wrapT = platform.SceneKit.SCNWrapModeClamp
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun createFillMaterial(
    red: Double,
    green: Double,
    blue: Double,
    alpha: Double
): SCNMaterial =
    SCNMaterial().apply {
        diffuse.contents = UIColor.colorWithRed(red, green, blue, alpha = alpha)
        lightingModelName = platform.SceneKit.SCNLightingModelConstant
        doubleSided = true
        readsFromDepthBuffer = true
        writesToDepthBuffer = false
        transparencyMode = platform.SceneKit.SCNTransparencyModeAOne
    }
