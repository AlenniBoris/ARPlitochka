package com.example.arplitka.features.floordetection.presentation.screen

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.example.arplitka.features.floordetection.R
import com.example.arplitka.features.floordetection.presentation.FloorArEvent
import com.example.arplitka.features.floordetection.presentation.components.ArSceneLayer
import com.example.arplitka.features.floordetection.presentation.utils.decodePavingBitmap
import com.example.arplitka.features.floordetection.presentation.utils.loadTileTextureBitmap
import com.example.arplitka.features.floordetection.presentation.utils.mappers.toText
import com.example.arplitka.features.floordetection.presentation.viewmodel.FloorArViewModel
import com.example.arplitka.shared.ar.contracts.model.ArPoint3D
import com.example.arplitka.shared.ar.domain.geometry.buildAlignedSectionGeometry
import com.example.arplitka.shared.ar.domain.model.FloorWorkflowStage
import com.example.arplitka.shared.ui.kit.ar.ArActionRail
import com.example.arplitka.shared.ui.kit.ar.ArApplyingOverlay
import com.example.arplitka.shared.ui.kit.ar.ArCompactHint
import com.example.arplitka.shared.ui.kit.ar.ArColorRail
import com.example.arplitka.shared.ui.kit.ar.ArContourActionButtons
import com.example.arplitka.shared.ui.kit.ar.ArDebugTogglePanel
import com.example.arplitka.shared.ui.kit.ar.ArTilePickerBottomSheet
import com.example.arplitka.shared.ui.kit.ar.ArTransientMessage
import com.example.arplitka.shared.ui.kit.ar.ArTopBar
import com.example.arplitka.shared.ui.kit.ar.ArZoneResetButton
import com.example.arplitka.shared.ui.core.model.toUiModel
import com.example.arplitka.shared.ui.kit.ar.BlockingMessage
import com.example.arplitka.shared.ui.kit.ar.CenterReticle
import com.example.arplitka.shared.ui.kit.utils.isDebugBuild
import com.example.arplitka.shared.ui.navigation.AppNavigator
import com.google.ar.core.Frame
import com.google.ar.core.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

@Composable
fun FloorArScreen(
    navigator: AppNavigator,
    initialTileId: Long? = null,
    initialLayoutId: String? = null,
    initialPaletteId: String? = null,
    viewModel: FloorArViewModel = koinViewModel {
        org.koin.core.parameter.parametersOf(initialTileId, initialLayoutId, initialPaletteId)
    }
) {
    val uiState by viewModel.uiState.collectAsState()
    val event by remember { mutableStateOf(viewModel.event) }

    LaunchedEffect(event) {
        launch {
            event.filterIsInstance<FloorArEvent.NavigateBack>()
                .collect { navigator.backFromAr(it.returnToTileId) }
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.reset() }
    }

    BackHandler { viewModel.onBack() }

    FloorArContent(
        uiState = uiState,
        onBack = viewModel::onBack,
        onSessionUpdated = viewModel::onSessionUpdated,
        onAddPoint = viewModel::addPoint,
        onUndoPoint = viewModel::undoPoint,
        onConfirmContour = viewModel::confirmContour,
        onToggleTileVisibility = viewModel::toggleTileVisibility,
        onOpenTilePicker = viewModel::openTilePicker,
        onCloseTilePicker = viewModel::closeTilePicker,
        onPickerTileSelected = viewModel::onPickerTileSelected,
        onPickerLayoutSelected = viewModel::onPickerLayoutSelected,
        onPickerPaletteSelected = viewModel::onPickerPaletteSelected,
        onColorRailPaletteSelected = viewModel::onColorRailPaletteSelected,
        onClearSection = viewModel::clearSection,
        onRotateTexture = viewModel::rotateTexture,
        onToggleDebugPanel = viewModel::toggleDebugPanel,
        onClearUserMessage = viewModel::clearUserMessage,
        onRetryCatalogLoad = viewModel::retryCatalogLoad,
        onTileTextureApplied = viewModel::onTileTextureApplied
    )
}

@Composable
private fun FloorArContent(
    uiState: com.example.arplitka.features.floordetection.domain.model.FloorUiState,
    onBack: () -> Unit,
    onSessionUpdated: (Session, Frame, IntSize) -> Unit,
    onAddPoint: () -> Unit,
    onUndoPoint: () -> Unit,
    onConfirmContour: () -> Unit,
    onToggleTileVisibility: () -> Unit,
    onOpenTilePicker: () -> Unit,
    onCloseTilePicker: () -> Unit,
    onPickerTileSelected: (Long) -> Unit,
    onPickerLayoutSelected: (String) -> Unit,
    onPickerPaletteSelected: (String) -> Unit,
    onColorRailPaletteSelected: (String) -> Unit,
    onClearSection: () -> Unit,
    onRotateTexture: () -> Unit,
    onToggleDebugPanel: () -> Unit,
    onClearUserMessage: () -> Unit,
    onRetryCatalogLoad: () -> Unit,
    onTileTextureApplied: (Int) -> Unit
) {
    val context = LocalContext.current
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var sessionErrorMessage by remember { mutableStateOf<String?>(null) }
    var pavingBitmap by remember(uiState.arTileTexture, uiState.selectedTileType, uiState.stage) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(uiState.arTileTexture, uiState.selectedTileType, uiState.stage) {
        if (uiState.stage != FloorWorkflowStage.TILE_LAYOUT) {
            pavingBitmap = null
            return@LaunchedEffect
        }

        val textureUrl = uiState.arTileTexture?.textureUrl
        pavingBitmap = withContext(Dispatchers.IO) {
            if (!textureUrl.isNullOrBlank()) {
                loadTileTextureBitmap(context, textureUrl)
            } else {
                try {
                    context.assets.open(uiState.selectedTileType.assetPath).use { decodePavingBitmap(it) }
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    val hintText = uiState.compactHint
        ?: when (uiState.stage) {
            FloorWorkflowStage.SEARCHING_FLOOR -> uiState.instruction.toText()
            FloorWorkflowStage.CONTOUR_CLOSED -> uiState.instruction.toText()
            else -> null
        }

  Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewportSize = it }
    ) {
        key(uiState.arSessionResetKey) {
            ArSceneLayer(
                uiState = uiState,
                pavingBitmap = pavingBitmap,
                onSessionUpdated = { session, frame ->
                    onSessionUpdated(session, frame, viewportSize)
                },
                onSessionFailed = { exception ->
                    sessionErrorMessage = exception.localizedMessage ?: "AR session failed"
                },
                onSizeChanged = { viewportSize = it },
                onTileTextureApplied = onTileTextureApplied,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ArTopBar(
                onBack = onBack,
                backTitle = uiState.selectedTileName
            )

            ArZoneResetButton(
                onClick = onClearSection,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        ArCompactHint(
            text = hintText.orEmpty(),
            visible = hintText != null && uiState.stage != FloorWorkflowStage.TILE_LAYOUT,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp)
                .padding(horizontal = 16.dp)
        )

        if (!uiState.isContourConfirmed) {
            CenterReticle(
                modifier = Modifier.align(Alignment.Center),
                isActive = uiState.hasCenterHit &&
                    uiState.stage.ordinal < FloorWorkflowStage.CONTOUR_CONFIRMED.ordinal
            )
        }

        when (uiState.stage) {
            FloorWorkflowStage.PLACEMENT_EMPTY,
            FloorWorkflowStage.PLACEMENT_ACTIVE,
            FloorWorkflowStage.CONTOUR_CLOSED -> {
                ArContourActionButtons(
                    hasCenterHit = uiState.hasCenterHit,
                    isPolygonClosed = uiState.isPolygonClosed,
                    hasPoints = uiState.points.isNotEmpty(),
                    onAddPoint = if (uiState.isPolygonClosed) onConfirmContour else onAddPoint,
                    onUndoPoint = onUndoPoint,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp)
                )
            }
            else -> Unit
        }

        val showTileActions = uiState.stage.ordinal >= FloorWorkflowStage.CONTOUR_CONFIRMED.ordinal
        if (showTileActions) {
            ArActionRail(
                showAddOrChooseTile = true,
                showRemoveTile = uiState.stage == FloorWorkflowStage.TILE_LAYOUT,
                showRotate = uiState.stage == FloorWorkflowStage.TILE_LAYOUT,
                showRescan = false,
                onAddOrChooseTile = {
                    if (uiState.arTileTexture != null && !uiState.isTileVisible) {
                        onToggleTileVisibility()
                    } else if (uiState.isTileVisible) {
                        onOpenTilePicker()
                    } else {
                        onOpenTilePicker()
                    }
                },
                onRemoveTile = onToggleTileVisibility,
                onRotate = onRotateTexture,
                onRescan = onClearSection,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        if (uiState.stage == FloorWorkflowStage.TILE_LAYOUT && uiState.colorRailPalettes.isNotEmpty()) {
            ArColorRail(
                palettes = uiState.colorRailPalettes,
                onPaletteSelected = onColorRailPaletteSelected,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 56.dp)
            )
        }

        ArTilePickerBottomSheet(
            state = uiState.tilePicker,
            onDismiss = onCloseTilePicker,
            onTileSelected = onPickerTileSelected,
            onLayoutSelected = onPickerLayoutSelected,
            onPaletteSelected = onPickerPaletteSelected,
            onRetryCatalog = onRetryCatalogLoad
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
            ArDebugTogglePanel(
                showDebugPanel = uiState.showDebugPanel,
                onToggleDebug = onToggleDebugPanel,
                debugLines = mapOf(
                    stringResource(R.string.debug_planes) to uiState.horizontalPlaneCount.toString(),
                    stringResource(R.string.debug_area) to stringResource(R.string.area_format, uiState.selectedArea),
                    stringResource(R.string.debug_fill_bounds) to fillBoundsLabel,
                    stringResource(R.string.debug_tracking) to uiState.trackingState.name,
                    "Stage" to uiState.stage.name,
                    "Points" to uiState.points.size.toString(),
                    "Closed" to uiState.isPolygonClosed.toString(),
                    "Confirmed" to uiState.isContourConfirmed.toString(),
                    "Tile" to if (uiState.stage == FloorWorkflowStage.TILE_LAYOUT) "On" else "Off",
                    "Texture" to (uiState.arTileTexture?.textureUrl ?: uiState.selectedTileType.assetPath),
                    "Palette" to (uiState.tileSelection?.paletteId ?: "-"),
                ),
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }

        if (sessionErrorMessage != null) {
            BlockingMessage(
                title = stringResource(R.string.ar_not_available),
                message = sessionErrorMessage!!,
                modifier = Modifier.fillMaxSize()
            )
        }

        ArTransientMessage(
            exception = uiState.userException?.toUiModel(),
            onDismiss = onClearUserMessage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )

        ArApplyingOverlay(
            visible = uiState.isTileApplying,
            modifier = Modifier.fillMaxSize()
        )
    }
}
