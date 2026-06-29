package com.example.arplitka.iosapp.platform.render

import com.example.arplitka.iosapp.bridge.pg_create_contour_distance_label_image
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
import com.example.arplitka.shared.ar.domain.logic.FloorGeometry
import com.example.arplitka.shared.ar.domain.model.FloorContourUiState
import com.example.arplitka.shared.ar.domain.model.TextureRotation
import com.example.arplitka.shared.ar.domain.model.TileType
import kotlinx.cinterop.ExperimentalForeignApi
import platform.SceneKit.SCNCylinder
import platform.SceneKit.SCNMaterial
import platform.SceneKit.SCNMatrix4MakeScale
import platform.SceneKit.SCNNode
import platform.SceneKit.SCNPlane
import platform.SceneKit.SCNVector3Make
import platform.SceneKit.SCNWrapModeRepeat
import platform.UIKit.UIColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import com.example.arplitka.shared.ui.kit.utils.textureRepeatMeters
import com.example.arplitka.shared.ui.kit.utils.textureUrlToResourceStem

private const val POINT_RADIUS_M = 0.016f
private const val POINT_HEIGHT_M = 0.002f
private const val POINT_VISUAL_OFFSET_M = 0.008f
private const val LINE_WIDTH_M = 0.012f
private const val LINE_VISUAL_OFFSET_M = 0.003f
private const val FILL_VISUAL_OFFSET_M = 0.004f
private const val LOD_POINT_COUNT_THRESHOLD = 30
private const val LOD_POINT_RADIUS_SCALE = 0.85f
private const val LOD_LINE_WIDTH_SCALE = 0.80f
private const val POSITION_QUANT_M = 0.005f // Increased to 5mm to ignore sensor noise
private const val TILE_WIDTH_M = 0.78f
private const val TILE_HEIGHT_M = 1.04f

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
    private val asyncGeometryBuilder = AsyncContourGeometryBuilder()
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
    private var externalTileTexture: com.example.arplitka.shared.ar.contracts.model.ArTileTexture? = null

    fun setExternalTileTexture(texture: com.example.arplitka.shared.ar.contracts.model.ArTileTexture?) {
        externalTileTexture = texture
        tileMaterialCache.clear()
        lastContourStateKey = Int.MIN_VALUE
        lastFillBatchKey = Int.MIN_VALUE
    }

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
        asyncGeometryBuilder.cancelAll()
    }

    fun lastStructureKey(): Int = lastContourStateKey

    fun syncIfChanged(
        state: FloorContourUiState,
        lockedAlignedGeometry: AlignedSectionGeometry? = null,
        anchorOrigin: ArPoint3D? = null
    ): Boolean {
        val structureKey = contourStructureKey(state, lockedAlignedGeometry, anchorOrigin)
        val structureChanged = structureKey != lastContourStateKey
        if (!structureChanged) {
            return false
        }

        lastContourStateKey = structureKey
        lastFillBatchKey = Int.MIN_VALUE
        lastLineBatchKey = Int.MIN_VALUE
        lastDistanceLabelBatchKey = Int.MIN_VALUE

        syncContour(state, lockedAlignedGeometry, anchorOrigin)
        return true
    }

    private fun syncContour(
        state: FloorContourUiState,
        lockedAlignedGeometry: AlignedSectionGeometry?,
        anchorOrigin: ArPoint3D?
    ) {
        if (rootNode == null) return
        val floorY = state.placedPoints.firstOrNull()?.position?.yMeters
        val lodActive = state.placedPoints.size >= LOD_POINT_COUNT_THRESHOLD
        syncSectionFill(state, floorY, lockedAlignedGeometry, anchorOrigin)
        syncBatchedLines(state, floorY, lodActive)
        syncDistanceLabels(state, floorY)
        syncPointNodes(state, floorY, lodActive)
    }

    private fun syncSectionFill(
        state: FloorContourUiState,
        floorY: Float?,
        lockedAlignedGeometry: AlignedSectionGeometry?,
        anchorOrigin: ArPoint3D?
    ) {
        if (!state.showSectionFill || state.placedPoints.size < 3) {
            sectionFillNode?.hidden = true
            lastFillBatchKey = Int.MIN_VALUE
            return
        }

        val points = state.placedPoints.map { it.position }
        val sectionY = floorY ?: points.first().yMeters
        val coplanarPoints = points.map { FloorGeometry.projectToSectionFloor(it, sectionY) }
        
        // Use locked rotation if available, otherwise calculate current
        val aligned = if (state.isTileVisible) {
            lockedAlignedGeometry ?: buildAlignedSectionGeometry(coplanarPoints)
        } else {
            null
        }

        val precision = if (state.isFinalized) 10000f else 500f
        
        // STABILITY FIX: Use a lower precision for the batch key to avoid rebuilding 
        // geometry on every 0.1mm jitter. 2mm (500f) is enough for visual stability.
        val keyPrecision = 500f 
        
        val fillBatchKey = fillBatchKey(
            localPoints = aligned?.localPoints,
            worldPoints = coplanarPoints,
            isFinalized = state.isFinalized,
            isTileVisible = state.isTileVisible,
            selectedTileType = state.selectedTileType,
            textureRotation = state.textureRotation,
            aligned = aligned,
            anchorOrigin = anchorOrigin,
            precision = keyPrecision
        )

        val yBase = sectionY + FILL_VISUAL_OFFSET_M
        val node = sectionFillNode ?: SCNNode().also { created ->
            sectionFillNode = created
            rootNode?.addChildNode(created)
        }

        // Separate material update from geometry rebuild
        val targetMaterial = if (aligned != null) {
            resolveFillMaterial(state, aligned)
        } else {
            fillMaterial
        }

        if (fillBatchKey == lastFillBatchKey) {
            node.hidden = false
            node.position = SCNVector3Make(0f, yBase, 0f)
            node.eulerAngles = SCNVector3Make(0f, 0f, 0f)
            if (node.geometry?.firstMaterial != targetMaterial) {
                node.geometry?.firstMaterial = targetMaterial
            }
            return
        }

        lastFillBatchKey = fillBatchKey
        node.hidden = false
        node.position = SCNVector3Make(0f, yBase, 0f)
        node.eulerAngles = SCNVector3Make(0f, 0f, 0f)
        if (node.geometry?.firstMaterial != targetMaterial) {
            node.geometry?.firstMaterial = targetMaterial
        }

        val fillPointCount = coplanarPoints.size
        val pointBuffer = FloatArray(fillPointCount * 2)
        coplanarPoints.forEachIndexed { index, point ->
            val offset = index * 2
            pointBuffer[offset] = point.xMeters
            pointBuffer[offset + 1] = point.zMeters
        }

        val uvBuffer = FloatArray(fillPointCount * 2)
        val centroidU: Float
        val centroidV: Float
        val centroidX: Float
        val centroidZ: Float

        if (aligned != null) {
            centroidX = anchorOrigin?.xMeters ?: aligned.centroidX
            centroidZ = anchorOrigin?.zMeters ?: aligned.centroidZ
            centroidU = 0f
            centroidV = 0f

            val baseAxisX = aligned.axisX
            val baseAxisZ = aligned.axisZ
            val basePerpX = aligned.perpendicularX
            val basePerpZ = aligned.perpendicularZ

            val angleRad = eulerDegreesToRadians(state.textureRotation.degrees.toFloat())
            val cosR = cos(angleRad.toDouble()).toFloat()
            val sinR = sin(angleRad.toDouble()).toFloat()

            val axisX = baseAxisX * cosR - basePerpX * sinR
            val axisZ = baseAxisZ * cosR - basePerpZ * sinR
            val perpX = baseAxisX * sinR + basePerpX * cosR
            val perpZ = baseAxisZ * sinR + basePerpZ * cosR

            coplanarPoints.forEachIndexed { index, point ->
                val dx = point.xMeters - centroidX
                val dz = point.zMeters - centroidZ
                uvBuffer[index * 2] = dx * axisX + dz * axisZ
                uvBuffer[index * 2 + 1] = dx * perpX + dz * perpZ
            }
        } else {
            centroidX = coplanarPoints.map { it.xMeters }.average().toFloat()
            centroidZ = coplanarPoints.map { it.zMeters }.average().toFloat()
            centroidU = 0.5f
            centroidV = 0.5f
            coplanarPoints.forEachIndexed { index, _ ->
                uvBuffer[index * 2] = 0.5f
                uvBuffer[index * 2 + 1] = 0.5f
            }
        }

        asyncGeometryBuilder.requestFillBuild(
            batchKey = fillBatchKey,
            request = ContourFillBuildRequest(
                pointBuffer = pointBuffer,
                uvBuffer = uvBuffer,
                fillPointCount = fillPointCount,
                centroidX = centroidX,
                centroidZ = centroidZ,
                centroidU = centroidU,
                centroidV = centroidV,
                yM = 0f
            )
        ) { readyBatchKey, geometry ->
            if (readyBatchKey != lastFillBatchKey) return@requestFillBuild
            if (geometry == null) {
                node.hidden = true
                return@requestFillBuild
            }
            geometry.materials = listOf(targetMaterial)
            node.geometry = geometry
            node.position = SCNVector3Make(0f, yBase, 0f)
            node.eulerAngles = SCNVector3Make(0f, 0f, 0f)
            node.renderingOrder = 5
            node.hidden = false
        }
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
        val node = batchedLinesNode ?: SCNNode().also { created ->
            batchedLinesNode = created
            rootNode?.addChildNode(created)
        }

        if (lineBatchKey == lastLineBatchKey) {
            node.hidden = false
            return
        }

        lastLineBatchKey = lineBatchKey
        node.hidden = false

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

        asyncGeometryBuilder.requestLinesBuild(
            batchKey = lineBatchKey,
            request = ContourLinesBuildRequest(
                segmentBuffer = segmentBuffer,
                segmentCount = segmentCount,
                yM = yBase,
                halfWidthM = halfWidth
            )
        ) { readyBatchKey, geometry ->
            if (readyBatchKey != lastLineBatchKey) return@requestLinesBuild
            if (geometry == null) {
                node.hidden = true
                return@requestLinesBuild
            }
            node.geometry = geometry
            node.geometry?.materials = listOf(lineMaterial)
            node.renderingOrder = 6
            node.hidden = false
        }
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
        val structureChanged = labelBatchKey != lastDistanceLabelBatchKey
        if (structureChanged) {
            lastDistanceLabelBatchKey = labelBatchKey
        }

        segments.forEach { segment ->
            val labelText = segment.distanceMeters.formatContourDistanceMeters()
            val node = distanceLabelNodes.getOrPut(segment.key) {
                SCNNode().also { created ->
                    rootNode?.addChildNode(created)
                }
            }
            if (lastDistanceLabelTexts[segment.key] != labelText) {
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

    private fun contourStructureKey(
        state: FloorContourUiState,
        lockedAlignedGeometry: AlignedSectionGeometry?,
        anchorOrigin: ArPoint3D?
    ): Int {
        var key = state.placedPoints.size * 31
        
        // Use high precision for finalized state to avoid quantization jumps
        val precision = if (state.isFinalized) 10000f else 500f // 0.1mm vs 2mm
        
        state.placedPoints.forEach { point ->
            key = key * 31 + (point.position.xMeters * precision).roundToInt()
            key = key * 31 + (point.position.zMeters * precision).roundToInt()
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
        if (lockedAlignedGeometry != null) {
            key = key * 31 + (lockedAlignedGeometry.centroidX * precision).roundToInt()
            key = key * 31 + (lockedAlignedGeometry.centroidZ * precision).roundToInt()
            key = key * 31 + (lockedAlignedGeometry.rotationYDegrees * 10f).roundToInt()
        }
        if (anchorOrigin != null) {
            key = key * 31 + (anchorOrigin.xMeters * precision).roundToInt()
            key = key * 31 + (anchorOrigin.zMeters * precision).roundToInt()
        }
        return key
    }

    private fun resolveFillMaterial(
        state: FloorContourUiState,
        aligned: AlignedSectionGeometry
    ): SCNMaterial {
        if (!state.isTileVisible) return fillMaterial

        val external = externalTileTexture
        if (external != null) {
            val stem = textureUrlToResourceStem(external.textureUrl)
            val widthM = textureRepeatMeters(external.repeatWidthMm)
            val heightM = textureRepeatMeters(external.repeatLengthMm)
            val rotation = state.textureRotation.degrees + external.rotationDegrees
            val cacheKey = TileMaterialKey.External(
                resourceStem = stem,
                widthM = widthM,
                heightM = heightM,
                rotationDegrees = rotation
            )
            return tileMaterialCache.getOrPut(cacheKey) {
                val image = pg_create_tile_section_pattern_image(stem, widthM, heightM, rotation)
                if (image != null) {
                    SCNMaterial().apply {
                        diffuse.contents = image
                        lightingModelName = platform.SceneKit.SCNLightingModelConstant
                        doubleSided = true
                        readsFromDepthBuffer = true
                        writesToDepthBuffer = false
                        diffuse.wrapS = SCNWrapModeRepeat
                        diffuse.wrapT = SCNWrapModeRepeat
                    }
                } else {
                    createTileMaterial(resourceName = state.selectedTileType.resourceName)
                }
            }
        }

        val cacheKey = TileMaterialKey.Legacy(tileType = state.selectedTileType)
        return tileMaterialCache.getOrPut(cacheKey) {
            createTileMaterial(
                resourceName = state.selectedTileType.resourceName
            )
        }
    }

    private fun fillBatchKey(
        localPoints: List<com.example.arplitka.shared.ar.contracts.model.ArPoint2D>?,
        worldPoints: List<ArPoint3D>,
        isFinalized: Boolean,
        isTileVisible: Boolean,
        selectedTileType: TileType,
        textureRotation: TextureRotation,
        aligned: AlignedSectionGeometry?,
        anchorOrigin: ArPoint3D?,
        precision: Float
    ): Int {
        var key = worldPoints.size * 31
        key = key * 31 + if (isFinalized) 1 else 0
        key = key * 31 + if (isTileVisible) 1 else 0
        key = key * 31 + selectedTileType.ordinal
        key = key * 31 + textureRotation.ordinal

        if (aligned != null) {
            // Rotation is critical for the batch key
            key = key * 31 + (aligned.rotationYDegrees * 100f).roundToInt()
        }
        
        if (anchorOrigin != null) {
            // CRITICAL: Rebuild geometry if anchor moves, to update baked UVs
            key = key * 31 + (anchorOrigin.xMeters * precision).roundToInt()
            key = key * 31 + (anchorOrigin.zMeters * precision).roundToInt()
        }

        worldPoints.forEach { point ->
            key = key * 31 + (point.xMeters * precision).roundToInt()
            key = key * 31 + (point.zMeters * precision).roundToInt()
        }
        key = key * 31 + ((worldPoints.firstOrNull()?.yMeters ?: 0f) * precision).roundToInt()
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

    private data class TileMaterialKey private constructor(
        val kind: Int,
        val tileTypeOrdinal: Int = 0,
        val resourceStem: String = "",
        val widthM: Float = 0f,
        val heightM: Float = 0f,
        val rotationDegrees: Float = 0f
    ) {
        companion object {
            fun Legacy(tileType: TileType): TileMaterialKey =
                TileMaterialKey(kind = 0, tileTypeOrdinal = tileType.ordinal)

            fun External(
                resourceStem: String,
                widthM: Float,
                heightM: Float,
                rotationDegrees: Float
            ): TileMaterialKey = TileMaterialKey(
                kind = 1,
                resourceStem = resourceStem,
                widthM = widthM,
                heightM = heightM,
                rotationDegrees = rotationDegrees
            )
        }
    }

    private fun eulerDegreesToRadians(degrees: Float): Float = degrees * PI.toFloat() / 180f

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

    private fun createLabelMaterial(): SCNMaterial =
        SCNMaterial().apply {
            lightingModelName = platform.SceneKit.SCNLightingModelConstant
            doubleSided = true
            readsFromDepthBuffer = true
            writesToDepthBuffer = false
        }

    private fun createTileMaterial(
        resourceName: String
    ): SCNMaterial {
        val image = pg_load_tile_texture_image(resourceName, 0f) // Load without rotation
        return SCNMaterial().apply {
            diffuse.contents = image
            lightingModelName = platform.SceneKit.SCNLightingModelConstant
            doubleSided = true
            readsFromDepthBuffer = true
            writesToDepthBuffer = false
            diffuse.wrapS = SCNWrapModeRepeat
            diffuse.wrapT = SCNWrapModeRepeat
            // Scale texture to match real world meters (0.78m x 1.04m)
            diffuse.contentsTransform = SCNMatrix4MakeScale(
                (1.0 / TILE_WIDTH_M).toFloat(),
                (1.0 / TILE_HEIGHT_M).toFloat(),
                1.0f
            )
        }
    }

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
}
