package com.example.arplitka.features.floordetection.presentation.screen

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.arplitka.features.floordetection.R
import com.example.arplitka.shared.ar.domain.model.FloorWorkflowStage
import com.example.arplitka.features.floordetection.presentation.components.ArActionButtons
import com.example.arplitka.features.floordetection.presentation.components.ArSceneLayer
import com.example.arplitka.features.floordetection.presentation.components.ArStatusOverlay
import com.example.arplitka.features.floordetection.presentation.utils.decodePavingBitmap
import com.example.arplitka.features.floordetection.presentation.viewmodel.FloorArViewModel
import com.example.arplitka.shared.ar.contracts.model.ArPoint3D
import com.example.arplitka.shared.ar.domain.geometry.buildAlignedSectionGeometry
import com.example.arplitka.shared.ui.kit.ar.BlockingMessage
import com.example.arplitka.shared.ui.kit.ar.CenterReticle
import com.example.arplitka.shared.ui.kit.ar.DebugPanel
import com.example.arplitka.shared.ui.kit.utils.isDebugBuild
import androidx.compose.foundation.layout.padding
import com.example.arplitka.shared.ui.kit.ar.ArTopBar
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FloorArScreen(
    viewModel: FloorArViewModel = koinViewModel(),
    onBack: () -> Unit = {}
) {
    BackHandler {
        viewModel.reset()
        onBack()
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.reset()
        }
    }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var sessionErrorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    var pavingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(context, uiState.stage, uiState.selectedTileType) {
        if (uiState.stage != FloorWorkflowStage.TILE_LAYOUT) {
            pavingBitmap = null
            return@LaunchedEffect
        }
        runCatching {
            pavingBitmap = withContext(Dispatchers.IO) {
                context.assets.open(uiState.selectedTileType.assetPath).use(::decodePavingBitmap)
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
        ArSceneLayer(
            uiState = uiState,
            pavingBitmap = pavingBitmap,
            onSessionUpdated = { session, frame ->
                viewModel.onSessionUpdated(session, frame, viewportSize)
            },
            onSessionFailed = { exception ->
                sessionErrorMessage = exception.localizedMessage ?: "AR session failed"
            },
            onSizeChanged = { viewportSize = it }
        )

        CenterReticle(
            modifier = Modifier.align(Alignment.Center),
            isActive = uiState.hasCenterHit && uiState.stage.ordinal < FloorWorkflowStage.CONTOUR_CONFIRMED.ordinal
        )

        ArTopBar(onBack = {
            viewModel.reset()
            onBack()
        })

        ArStatusOverlay(
            status = uiState.status,
            instruction = uiState.instruction,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        ArActionButtons(
            uiState = uiState,
            onAddPoint = { viewModel.addPoint() },
            onUndoPoint = { viewModel.undoPoint() },
            onConfirmContour = { viewModel.confirmContour() },
            onToggleTileVisibility = { viewModel.toggleTileVisibility() },
            onChangeTileType = { viewModel.changeTileType() },
            onRotateTexture = { viewModel.rotateTexture() },
            onClearSection = { viewModel.clearSection() },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (isDebugBuild()) {
            val fillBoundsLabel = if (uiState.points.size >= 3) {
                val aligned = buildAlignedSectionGeometry(
                    uiState.points.map { point ->
                        ArPoint3D(
                            xMeters = point.pose.tx(),
                            yMeters = point.pose.ty(),
                            zMeters = point.pose.tz()
                        )
                    }
                )
                val width = (aligned.boundsWidthM * 100).roundToInt() / 100f
                val height = (aligned.boundsHeightM * 100).roundToInt() / 100f
                "$width x $height m"
            } else {
                "-"
            }
            DebugPanel(
                debugLines = mapOf(
                    stringResource(R.string.debug_planes) to uiState.horizontalPlaneCount.toString(),
                    stringResource(R.string.debug_area) to stringResource(R.string.area_format, uiState.selectedArea),
                    stringResource(R.string.debug_fill_bounds) to fillBoundsLabel,
                    stringResource(R.string.debug_tracking) to uiState.trackingState.name,
                    "Stage" to uiState.stage.name,
                    "Points" to uiState.points.size.toString(),
                    "Closed" to uiState.isPolygonClosed.toString(),
                    "Confirmed" to uiState.isContourConfirmed.toString(),
                    "Show pts" to if (uiState.showContourPoints) "Yes" else "No",
                    "Show lines" to if (uiState.showContourLines) "Yes" else "No",
                    "Show fill" to if (uiState.showSectionFill) "Yes" else "No",
                    "Tile" to if (uiState.stage == FloorWorkflowStage.TILE_LAYOUT) "On" else "Off",
                    "Texture rotation" to (uiState.textureRotation.ordinal * 45).toString()
                ),
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
            )
        }

        if (sessionErrorMessage != null) {
            BlockingMessage(
                title = stringResource(R.string.ar_not_available),
                message = sessionErrorMessage!!,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
