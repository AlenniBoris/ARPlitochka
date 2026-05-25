package com.example.arplitka.features.floordetection.presentation.screen

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.arplitka.features.floordetection.BuildConfig
import com.example.arplitka.features.floordetection.R
import com.example.arplitka.features.floordetection.presentation.viewmodel.FloorArViewModel
import com.example.arplitka.shared.ui.BlockingMessage
import com.example.arplitka.shared.ui.CenterReticle
import com.example.arplitka.shared.ui.DebugPanel
import com.example.arplitka.shared.ui.StatusPanel
import com.example.arplitka.shared.ui.UiText
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import com.google.ar.core.Config
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.material.setBaseColorMap
import dev.romainguy.kotlin.math.Float2
import io.github.sceneview.math.Position2
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.ImageNode
import io.github.sceneview.node.ShapeNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.texture.setBitmap
import kotlin.math.atan2
import kotlin.math.sqrt
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FloorArScreen(
    viewModel: FloorArViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val cameraNode = rememberARCameraNode(engine)
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var sessionError by remember { mutableStateOf<UiText?>(null) }
    val context = LocalContext.current

    val pointMaterial = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF00E676)) }
    val snappingPointMaterial = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF69F0AE)) }
    val lineMaterial = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF2196F3)) }
    val previewLineMaterial = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF00A2FF)) }
    val fillMaterial = remember(materialLoader) { materialLoader.createColorInstance(Color(0x802196F3)) }

    var pavingMaterial by remember { mutableStateOf<MaterialInstance?>(null) }
    LaunchedEffect(materialLoader, context, engine) {
        runCatching {
            val bitmap = withContext(Dispatchers.IO) {
                val inputStream = context.assets.open("textures/paving_stones.png")
                BitmapFactory.decodeStream(inputStream)
            }
            
            if (bitmap != null) {
                val texture = Texture.Builder()
                    .width(bitmap.width)
                    .height(bitmap.height)
                    .sampler(Texture.Sampler.SAMPLER_2D)
                    .format(Texture.InternalFormat.SRGB8_A8)
                    .levels(1)
                    .build(engine)
                
                texture.setBitmap(engine, bitmap)
                
                // Самый базовый способ создания материала с текстурой в SceneView
                pavingMaterial = materialLoader.createTextureInstance(texture)
            }
        }.onFailure { 
            android.util.Log.e("FloorArScreen", "Async texture load failed", it)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewportSize = it }
    ) {
        ARSceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            cameraNode = cameraNode,
            planeRenderer = !uiState.isFinalized,
            sessionConfiguration = { session, config ->
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    config.depthMode = Config.DepthMode.AUTOMATIC
                }
                config.focusMode = Config.FocusMode.AUTO
                config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            },
            onSessionFailed = { exception ->
                sessionError = UiText.DynamicString(exception.localizedMessage ?: "AR session failed")
            },
            onSessionUpdated = { session, frame ->
                viewModel.onSessionUpdated(session, frame, viewportSize)
            }
        ) {
            val sectionFloorY = uiState.points.firstOrNull()?.pose?.ty()

            // Render filled area below lines and points
            if (uiState.isPolygonClosed && uiState.points.size >= MIN_POINTS_TO_FILL) {
                val centroidX = uiState.points.map { it.pose.tx() }.average().toFloat()
                val centroidZ = uiState.points.map { it.pose.tz() }.average().toFloat()

                val useTexture = uiState.isFinalized
                val material = if (useTexture) (pavingMaterial ?: fillMaterial) else fillMaterial
                
                val uvScale = if (useTexture) {
                    // Calculate bounding box for UV scaling
                    val minX = uiState.points.minOf { it.pose.tx() }
                    val maxX = uiState.points.maxOf { it.pose.tx() }
                    val minZ = uiState.points.minOf { it.pose.tz() }
                    val maxZ = uiState.points.maxOf { it.pose.tz() }
                    val width = maxX - minX
                    val height = maxZ - minZ
                    
                    // Physical texture size: 0.78m x 1.04m (780mm x 1040mm)
                    Float2(width / 0.78f, height / 1.04f)
                } else {
                    Float2(1f, 1f)
                }

                // Применяем масштаб UV через конструктор ShapeNode (безопаснее)
                ShapeNode(
                    polygonPath = uiState.points.map { point ->
                        Position2(
                            x = point.pose.tx() - centroidX,
                            y = centroidZ - point.pose.tz()
                        )
                    },
                    materialInstance = material,
                    uvScale = uvScale,
                    position = Position(
                        x = centroidX,
                        y = (sectionFloorY ?: 0f) + FILL_VISUAL_OFFSET_M,
                        z = centroidZ
                    ),
                    rotation = Rotation(x = -90f)
                )
            }

            // Render Lines
            if (uiState.points.size >= 2) {
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
                            size = Size(segment.visualLength, LINE_HEIGHT_M, LINE_WIDTH_M),
                            materialInstance = lineMaterial,
                            position = segment.midPosition,
                            rotation = Rotation(y = segment.rotationY)
                        )
                        val labelText = segment.measuredLength.formatMeters()
                        val labelBitmap = remember(labelText) { createDistanceLabelBitmap(labelText) }
                        ImageNode(
                            bitmap = labelBitmap,
                            size = Size(LABEL_WIDTH_M, LABEL_HEIGHT_M),
                            position = Position(
                                x = segment.midPosition.x,
                                y = segment.midPosition.y + LABEL_VISUAL_OFFSET_M,
                                z = segment.midPosition.z
                            ),
                            rotation = Rotation(x = -90f, y = readableLineRotationYDegrees(segment.dx, segment.dz))
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
                            size = Size(segment.visualLength, LINE_HEIGHT_M, LINE_WIDTH_M),
                            materialInstance = lineMaterial,
                            position = segment.midPosition,
                            rotation = Rotation(y = segment.rotationY)
                        )
                        val labelText = segment.measuredLength.formatMeters()
                        val labelBitmap = remember(labelText) { createDistanceLabelBitmap(labelText) }
                        ImageNode(
                            bitmap = labelBitmap,
                            size = Size(LABEL_WIDTH_M, LABEL_HEIGHT_M),
                            position = Position(
                                x = segment.midPosition.x,
                                y = segment.midPosition.y + LABEL_VISUAL_OFFSET_M,
                                z = segment.midPosition.z
                            ),
                            rotation = Rotation(x = -90f, y = readableLineRotationYDegrees(segment.dx, segment.dz))
                        )
                    }
                }
            }

            // Render Preview Line
            if (!uiState.isFinalized && uiState.points.isNotEmpty() && !uiState.isPolygonClosed && uiState.currentHitPose != null) {
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
                        size = Size(segment.visualLength, PREVIEW_LINE_HEIGHT_M, PREVIEW_LINE_WIDTH_M),
                        materialInstance = previewLineMaterial,
                        position = segment.midPosition,
                        rotation = Rotation(y = segment.rotationY)
                    )
                }
            }

            // Render points last so markers are always above fill and lines.
            uiState.points.forEachIndexed { index, point ->
                val pose = point.pose
                CylinderNode(
                    radius = POINT_RADIUS_M,
                    height = POINT_HEIGHT_M,
                    sideCount = 32,
                    materialInstance = if (uiState.snappedPointIndex == index) {
                        snappingPointMaterial
                    } else {
                        pointMaterial
                    },
                    position = Position(
                        x = pose.tx(),
                        y = (sectionFloorY ?: pose.ty()) + POINT_VISUAL_OFFSET_M,
                        z = pose.tz()
                    )
                )
            }
        }

        CenterReticle(modifier = Modifier.align(Alignment.Center), isActive = uiState.hasCenterHit)

        StatusPanel(
            statusText = uiState.statusText.asString(),
            instructionText = uiState.instructionText.asString(),
            modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 16.dp, vertical = 36.dp)
        )

        // Action Buttons
        if (!uiState.isFinalized) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Undo Button
                    if (uiState.points.isNotEmpty()) {
                        Button(
                            onClick = { viewModel.undoPoint() },
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black.copy(alpha = 0.6f),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.size(56.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.btn_undo)
                            )
                        }
                    }

                    // Add Point / OK Button
                    Button(
                        onClick = { viewModel.addPoint() },
                        enabled = uiState.hasCenterHit,
                        shape = RoundedCornerShape(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isPolygonClosed) Color(0xFF4CAF50) else Color.White,
                            contentColor = if (uiState.isPolygonClosed) Color.White else Color.Black,
                            disabledContainerColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.size(if (uiState.isPolygonClosed) 80.dp else 72.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isPolygonClosed) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = if (uiState.isPolygonClosed) stringResource(R.string.btn_ok) else stringResource(R.string.btn_add_point),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp)
            ) {
                Button(
                    onClick = { viewModel.clearSection() },
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.size(72.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.btn_clear_section),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        if (BuildConfig.DEBUG) {
            DebugPanel(
                debugLines = mapOf(
                    stringResource(R.string.debug_planes) to uiState.horizontalPlaneCount.toString(),
                    stringResource(R.string.debug_area) to stringResource(R.string.area_format, uiState.selectedArea),
                    stringResource(R.string.debug_tracking) to uiState.trackingState.name,
                    "Points" to uiState.points.size.toString(),
                    "Closed" to uiState.isPolygonClosed.toString(),
                    "Finalized" to uiState.isFinalized.toString()
                ),
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
            )
        }

        if (sessionError != null) {
            BlockingMessage(
                title = stringResource(R.string.ar_not_available),
                message = sessionError?.asString() ?: stringResource(R.string.ar_session_failed),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun lineRotationYDegrees(dx: Float, dz: Float): Float {
    return -Math.toDegrees(atan2(dz, dx).toDouble()).toFloat()
}

private fun readableLineRotationYDegrees(dx: Float, dz: Float): Float {
    val angle = lineRotationYDegrees(dx, dz)
    return when {
        angle > 90f -> angle - 180f
        angle < -90f -> angle + 180f
        else -> angle
    }
}

private data class SegmentGeometry(
    val measuredLength: Float,
    val visualLength: Float,
    val midPosition: Position,
    val dx: Float,
    val dz: Float,
    val rotationY: Float
)

private fun createSegmentGeometry(
    rawStart: Position,
    rawEnd: Position,
    y: Float,
    startInset: Float,
    endInset: Float
): SegmentGeometry? {
    val dx = rawEnd.x - rawStart.x
    val dz = rawEnd.z - rawStart.z
    val measuredLength = sqrt(dx * dx + dz * dz)
    val visualLength = measuredLength - startInset - endInset
    if (visualLength <= MIN_LINE_LENGTH_M) return null

    val ux = dx / measuredLength
    val uz = dz / measuredLength
    val start = Position(
        x = rawStart.x + ux * startInset,
        y = y,
        z = rawStart.z + uz * startInset
    )
    val end = Position(
        x = rawEnd.x - ux * endInset,
        y = y,
        z = rawEnd.z - uz * endInset
    )

    return SegmentGeometry(
        measuredLength = measuredLength,
        visualLength = visualLength,
        midPosition = Position(
            x = (start.x + end.x) / 2f,
            y = y,
            z = (start.z + end.z) / 2f
        ),
        dx = dx,
        dz = dz,
        rotationY = lineRotationYDegrees(dx, dz)
    )
}

private fun createDistanceLabelBitmap(text: String): Bitmap {
    val width = 320
    val height = 112
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        textSize = 38f
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    canvas.drawRoundRect(
        RectF(0f, 0f, width.toFloat(), height.toFloat()),
        28f,
        28f,
        backgroundPaint
    )
    val textBaseline = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(text, width / 2f, textBaseline, textPaint)
    return bitmap
}

private fun Float.formatMeters(): String {
    return String.format(Locale.getDefault(), "%.2f м", this)
}

private const val MIN_POINTS_TO_FILL = 3
private const val POINT_RADIUS_M = 0.016f
private const val POINT_HEIGHT_M = 0.002f
private const val POINT_VISUAL_OFFSET_M = 0.008f
private const val LINE_WIDTH_M = 0.012f
private const val LINE_HEIGHT_M = 0.002f
private const val LINE_VISUAL_OFFSET_M = 0.003f
private const val PREVIEW_LINE_WIDTH_M = 0.016f
private const val PREVIEW_LINE_HEIGHT_M = 0.002f
private const val PREVIEW_LINE_VISUAL_OFFSET_M = 0.005f
private const val FILL_VISUAL_OFFSET_M = 0.001f
private const val LABEL_WIDTH_M = 0.13f
private const val LABEL_HEIGHT_M = 0.045f
private const val LABEL_VISUAL_OFFSET_M = 0.018f
private const val MIN_LINE_LENGTH_M = 0.001f
