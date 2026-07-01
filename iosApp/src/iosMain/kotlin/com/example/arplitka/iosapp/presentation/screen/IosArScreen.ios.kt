package com.example.arplitka.iosapp.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import com.example.arplitka.iosapp.presentation.mapper.toIosText
import com.example.arplitka.iosapp.presentation.mapper.toReticleState
import com.example.arplitka.iosapp.presentation.model.IosArScreenModel
import com.example.arplitka.shared.ar.contracts.state.FloorArEvent
import com.example.arplitka.shared.ar.domain.model.FloorWorkflowStage
import com.example.arplitka.shared.tiles.domain.usecase.BuildArTileTextureUseCase
import com.example.arplitka.shared.tiles.domain.usecase.GetTilesUseCase
import com.example.arplitka.shared.ui.kit.ar.ArActionRail
import com.example.arplitka.shared.ui.kit.ar.ArApplyingOverlay
import com.example.arplitka.shared.ui.kit.ar.ArCompactHint
import com.example.arplitka.shared.ui.kit.ar.ArColorRail
import com.example.arplitka.shared.ui.kit.ar.ArContourActionButtons
import com.example.arplitka.shared.ui.kit.ar.ArDebugTogglePanel
import com.example.arplitka.shared.ui.kit.ar.ArTilePickerBottomSheet
import com.example.arplitka.shared.ui.kit.ar.ArTopBar
import com.example.arplitka.shared.ui.kit.ar.ArTransientMessage
import com.example.arplitka.shared.ui.kit.ar.ArZoneResetButton
import com.example.arplitka.shared.ui.kit.ar.CenterReticle
import com.example.arplitka.shared.ui.kit.utils.isDebugBuild
import com.example.arplitka.shared.ui.core.model.toUiModel
import com.example.arplitka.shared.ui.navigation.AppNavigator
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import platform.ARKit.ARSCNView
import platform.CoreGraphics.CGRectMake
import kotlin.math.roundToInt

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun IosArScreen(
    navigator: AppNavigator,
    initialTileId: Long?,
    initialLayoutId: String?,
    initialPaletteId: String?
) {
    val getTilesUseCase = koinInject<GetTilesUseCase>()
    val buildArTileTextureUseCase = koinInject<BuildArTileTextureUseCase>()
    val scope = rememberCoroutineScope()

    val model = remember {
        IosArScreenModel(
            getTilesUseCase = getTilesUseCase,
            buildArTileTextureUseCase = buildArTileTextureUseCase,
            scope = scope
        )
    }

    LaunchedEffect(initialTileId, initialLayoutId, initialPaletteId) {
        model.loadInitialSelection(initialTileId, initialLayoutId, initialPaletteId)
    }

    LaunchedEffect(model.contourState.isFinalized) {
        if (model.contourState.isFinalized) {
            model.onContourConfirmedWithAutoApply()
        }
    }

    val coordinator = model.coordinator
    val contourState = model.contourState
    val trackingStateName = model.trackingStateName
    val planeDebugMetrics = model.planeDebugMetrics
    val placementHint = model.placementHint
    val showContourRealignButton = model.showContourRealignButton
    val compactHint = model.compactHint
    val showDebugPanel = model.showDebugPanel
    val userException = model.userException

    DisposableEffect(model) {
        onDispose { model.pause() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        UIKitView(
            factory = {
                val sceneView = ARSCNView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0))
                coordinator.attach(sceneView)
                sceneView
            },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ArTopBar(
                onBack = { navigator.backFromAr(model.tileContext.tileSelection?.tileId) },
                backTitle = model.selectedTileName
            )

            ArZoneResetButton(
                onClick = model::resetArSessionAndSelection,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        val hintText = compactHint
            ?: placementHint
            ?: when (contourState.stage) {
                FloorWorkflowStage.SEARCHING_FLOOR -> contourState.instruction.toIosText()
                FloorWorkflowStage.CONTOUR_CLOSED -> contourState.instruction.toIosText()
                else -> null
            }

        ArCompactHint(
            text = hintText.orEmpty(),
            visible = hintText != null && contourState.stage != FloorWorkflowStage.TILE_LAYOUT,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp)
                .padding(horizontal = 16.dp)
        )

        CenterReticle(
            modifier = Modifier.align(Alignment.Center),
            isActive = contourState.hasCenterHit && contourState.showContourActions,
            state = contourState.toReticleState(
                placementHint = placementHint,
                placementStatus = planeDebugMetrics.placementStatus,
                isPlacementPlaceable = planeDebugMetrics.isPlacementPlaceable
            )
        )

        val showPlacementButtons = when (contourState.stage) {
            FloorWorkflowStage.PLACEMENT_EMPTY,
            FloorWorkflowStage.PLACEMENT_ACTIVE,
            FloorWorkflowStage.CONTOUR_CLOSED -> true
            FloorWorkflowStage.SEARCHING_FLOOR -> contourState.placedPoints.isNotEmpty()
            else -> false
        }

        if (showPlacementButtons) {
            ArContourActionButtons(
                hasCenterHit = contourState.hasCenterHit,
                isPolygonClosed = contourState.isPolygonClosed,
                hasPoints = contourState.placedPoints.isNotEmpty(),
                onAddPoint = { coordinator.dispatchEvent(FloorArEvent.AddPoint) },
                onUndoPoint = { coordinator.dispatchEvent(FloorArEvent.UndoPoint) },
                addContentDescription = "Добавить точку",
                undoContentDescription = "Отменить",
                okContentDescription = "Готово",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
            )
        }

        if (showContourRealignButton) {
            Button(
                onClick = { coordinator.applyContourRealignment() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp)
            ) {
                Text("Выровнять контур")
            }
        }

        val showTileActions = contourState.stage.ordinal >= FloorWorkflowStage.CONTOUR_CONFIRMED.ordinal
        if (showTileActions) {
            ArActionRail(
                showAddOrChooseTile = true,
                showRemoveTile = contourState.stage == FloorWorkflowStage.TILE_LAYOUT,
                showRotate = contourState.stage == FloorWorkflowStage.TILE_LAYOUT,
                showRescan = false,
                onAddOrChooseTile = { model.toggleTileVisibility() },
                onRemoveTile = { model.removeTileFill() },
                onRotate = { coordinator.dispatchEvent(FloorArEvent.RotateTexture) },
                onRescan = { coordinator.rescanSession() },
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        if (contourState.stage == FloorWorkflowStage.TILE_LAYOUT && model.colorRailPalettesState.isNotEmpty()) {
            ArColorRail(
                palettes = model.colorRailPalettesState,
                onPaletteSelected = model::onPickerPaletteSelected,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 56.dp)
            )
        }

        ArTilePickerBottomSheet(
            state = model.tilePickerState,
            onDismiss = model::closeTilePicker,
            onTileSelected = model::onPickerTileSelected,
            onLayoutSelected = model::onPickerLayoutSelected,
            onPaletteSelected = model::onPickerPaletteSelected,
            onRetryCatalog = model::retryCatalogLoad
        )

        if (isDebugBuild()) {
            ArDebugTogglePanel(
                showDebugPanel = showDebugPanel,
                onToggleDebug = model::toggleDebugPanel,
                debugLines = mapOf(
                    "Planes" to contourState.horizontalPlaneCount.toString(),
                    "Tracking" to trackingStateName,
                    "Points" to contourState.placedPoints.size.toString(),
                    "Finalized" to if (contourState.isFinalized) "Yes" else "No",
                    "Tile" to if (contourState.isTileVisible) "On" else "Off",
                    "Stage" to contourState.stage.name,
                    "Texture" to (model.tileContext.arTileTexture?.textureUrl ?: "-"),
                    "Palette" to (model.tileContext.tileSelection?.paletteId ?: "-"),
                ),
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }

        ArTransientMessage(
            exception = userException?.toUiModel(),
            onDismiss = model::clearUserMessage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )

        ArApplyingOverlay(
            visible = model.isTileApplying,
            modifier = Modifier.fillMaxSize()
        )
    }
}
