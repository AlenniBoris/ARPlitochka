package com.example.arplitka.features.floordetection.presentation.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import com.example.arplitka.features.floordetection.domain.model.FloorUiState
import com.example.arplitka.features.floordetection.domain.model.TextureRotation
import com.example.arplitka.features.floordetection.presentation.utils.*
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Texture
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.math.Position
import io.github.sceneview.node.*
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.texture.setBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ArSceneLayer(
    uiState: FloorUiState,
    pavingBitmap: Bitmap?,
    onSessionUpdated: (Session, Frame) -> Unit,
    onSessionFailed: (Exception) -> Unit,
    onSizeChanged: (IntSize) -> Unit,
    modifier: Modifier = Modifier
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val cameraNode = rememberARCameraNode(engine)

    val pointMaterial = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF00E676)) }
    val snappingPointMaterial = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF69F0AE)) }
    val lineMaterial = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF2196F3)) }
    val previewLineMaterial = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF00A2FF)) }
    val fillMaterial = remember(materialLoader) { materialLoader.createColorInstance(Color(0x802196F3)) }

    ARSceneView(
        modifier = modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        materialLoader = materialLoader,
        cameraNode = cameraNode,
        planeRenderer = uiState.showPlaneRenderer,
        sessionConfiguration = { session, config ->
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
            config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                config.depthMode = Config.DepthMode.AUTOMATIC
            }
            config.focusMode = Config.FocusMode.AUTO
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        },
        onSessionFailed = onSessionFailed,
        onSessionUpdated = onSessionUpdated
    ) {
        val sectionFloorY = uiState.points.firstOrNull()?.pose?.ty()

        // Render filled area
        if (uiState.showSectionFill) {
            val centroidX = uiState.points.map { it.pose.tx() }.average().toFloat()
            val centroidZ = uiState.points.map { it.pose.tz() }.average().toFloat()

            val useTexture = uiState.isTileVisible
            val sectionGeometry = uiState.points.toAlignedSectionGeometry(centroidX, centroidZ)
            val polygonBounds = sectionGeometry.polygonPath.bounds()
            var sectionMaterials by remember { mutableStateOf<Map<TextureRotation, MaterialInstance>>(emptyMap()) }

            if (useTexture) {
                LaunchedEffect(engine, materialLoader, pavingBitmap, polygonBounds.width, polygonBounds.height) {
                    val sourceBitmap = pavingBitmap ?: return@LaunchedEffect
                    sectionMaterials = withContext(Dispatchers.IO) {
                        TextureRotation.entries.associateWith { rotation ->
                            sourceBitmap.toSectionPatternBitmap(
                                widthMeters = polygonBounds.width,
                                heightMeters = polygonBounds.height,
                                rotationDegrees = rotation.ordinal * 45
                            )
                        }
                    }.mapValues { (_, bitmap) ->
                        val texture = Texture.Builder()
                            .width(bitmap.width)
                            .height(bitmap.height)
                            .sampler(Texture.Sampler.SAMPLER_2D)
                            .format(Texture.InternalFormat.SRGB8_A8)
                            .levels(1)
                            .build(engine)
                        texture.setBitmap(engine, bitmap)
                        materialLoader.createTextureInstance(texture)
                    }
                }
            }

            val material = if (useTexture) sectionMaterials[uiState.textureRotation] ?: fillMaterial else fillMaterial

            ShapeNode(
                polygonPath = sectionGeometry.polygonPath.map { Float2(it.x, it.y) },
                materialInstance = material,
                uvScale = Float2(1f, 1f),
                position = Float3(centroidX, (sectionFloorY ?: 0f) + FILL_VISUAL_OFFSET_M, centroidZ),
                rotation = Float3(-90f, sectionGeometry.rotationY, 0f)
            )
        }

        // Render Lines
        if (uiState.showContourLines) {
            for (i in 0 until uiState.points.size - 1) {
                val p1 = uiState.points[i].pose
                val p2 = uiState.points[i+1].pose
                val segment = createSegmentGeometry(
                    rawStart = Position(p1.tx(), p1.ty(), p1.tz()),
                    rawEnd = Position(p2.tx(), p2.ty(), p2.tz()),
                    y = (sectionFloorY ?: p1.ty()) + LINE_VISUAL_OFFSET_M,
                    startInset = POINT_RADIUS_M,
                    endInset = POINT_RADIUS_M
                )
                if (segment != null) {
                    CubeNode(
                        size = Float3(segment.visualLength, LINE_HEIGHT_M, LINE_WIDTH_M),
                        materialInstance = lineMaterial,
                        position = Float3(segment.midPosition.x, segment.midPosition.y, segment.midPosition.z),
                        rotation = Float3(0f, segment.rotationY, 0f)
                    )
                    val labelText = segment.measuredLength.formatMeters()
                    val labelBitmap = remember(labelText) { createDistanceLabelBitmap(labelText) }
                    ImageNode(
                        bitmap = labelBitmap,
                        size = Float3(LABEL_WIDTH_M, LABEL_HEIGHT_M, 0f),
                        position = Float3(segment.midPosition.x, segment.midPosition.y + LABEL_VISUAL_OFFSET_M, segment.midPosition.z),
                        rotation = Float3(-90f, readableLineRotationYDegrees(segment.dx, segment.dz), 0f)
                    )
                }
            }
            if (uiState.isPolygonClosed) {
                val p1 = uiState.points.last().pose
                val p2 = uiState.points.first().pose
                val segment = createSegmentGeometry(
                    rawStart = Position(p1.tx(), p1.ty(), p1.tz()),
                    rawEnd = Position(p2.tx(), p2.ty(), p2.tz()),
                    y = (sectionFloorY ?: p1.ty()) + LINE_VISUAL_OFFSET_M,
                    startInset = POINT_RADIUS_M,
                    endInset = POINT_RADIUS_M
                )
                if (segment != null) {
                    CubeNode(
                        size = Float3(segment.visualLength, LINE_HEIGHT_M, LINE_WIDTH_M),
                        materialInstance = lineMaterial,
                        position = Float3(segment.midPosition.x, segment.midPosition.y, segment.midPosition.z),
                        rotation = Float3(0f, segment.rotationY, 0f)
                    )
                    val labelText = segment.measuredLength.formatMeters()
                    val labelBitmap = remember(labelText) { createDistanceLabelBitmap(labelText) }
                    ImageNode(
                        bitmap = labelBitmap,
                        size = Float3(LABEL_WIDTH_M, LABEL_HEIGHT_M, 0f),
                        position = Float3(segment.midPosition.x, segment.midPosition.y + LABEL_VISUAL_OFFSET_M, segment.midPosition.z),
                        rotation = Float3(-90f, readableLineRotationYDegrees(segment.dx, segment.dz), 0f)
                    )
                }
            }
        }

        // Render Preview Line
        if (uiState.showPreviewLine) {
            val start = uiState.points.last().pose
            val endPose = uiState.currentHitPose!!
            val segment = createSegmentGeometry(
                rawStart = Position(start.tx(), start.ty(), start.tz()),
                rawEnd = Position(endPose.tx(), endPose.ty(), endPose.tz()),
                y = (sectionFloorY ?: start.ty()) + PREVIEW_LINE_VISUAL_OFFSET_M,
                startInset = POINT_RADIUS_M,
                endInset = 0f
            )
            if (segment != null) {
                CubeNode(
                    size = Float3(segment.visualLength, PREVIEW_LINE_HEIGHT_M, PREVIEW_LINE_WIDTH_M),
                    materialInstance = previewLineMaterial,
                    position = Float3(segment.midPosition.x, segment.midPosition.y, segment.midPosition.z),
                    rotation = Float3(0f, segment.rotationY, 0f)
                )
            }
        }

        // Render points
        if (uiState.showContourPoints) {
            uiState.points.forEachIndexed { index, point ->
                val pose = point.pose
                CylinderNode(
                    radius = POINT_RADIUS_M,
                    height = POINT_HEIGHT_M,
                    sideCount = 32,
                    materialInstance = if (uiState.snappedPointIndex == index) snappingPointMaterial else pointMaterial,
                    position = Float3(pose.tx(), (sectionFloorY ?: pose.ty()) + POINT_VISUAL_OFFSET_M, pose.tz())
                )
            }
        }
    }
}
