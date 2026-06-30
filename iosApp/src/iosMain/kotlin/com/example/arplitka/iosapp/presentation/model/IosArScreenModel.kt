package com.example.arplitka.iosapp.presentation.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.arplitka.iosapp.presentation.debug.IosPlaneDebugMetrics
import com.example.arplitka.iosapp.platform.ar.IosArSessionCoordinator
import com.example.arplitka.shared.core.domain.model.CommonException
import com.example.arplitka.shared.ar.domain.FloorArController
import com.example.arplitka.shared.ar.domain.model.FloorContourUiState
import com.example.arplitka.shared.tiles.domain.usecase.BuildArTileTextureUseCase
import com.example.arplitka.shared.tiles.domain.usecase.GetTilesUseCase
import com.example.arplitka.shared.ui.kit.ar.ArTilePickerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class IosArScreenModel(
    private val getTilesUseCase: GetTilesUseCase,
    private val buildArTileTextureUseCase: BuildArTileTextureUseCase,
    private val scope: CoroutineScope
) {
    var contourState by mutableStateOf(FloorContourUiState())
        private set

    var trackingStateName by mutableStateOf("INITIALIZING")
        private set

    var planeDebugMetrics by mutableStateOf(IosPlaneDebugMetrics())
        private set

    var placementHint by mutableStateOf<String?>(null)
        private set

    var showContourRealignButton by mutableStateOf(false)
        private set

    var compactHint by mutableStateOf<String?>(null)
        private set

    var showDebugPanel by mutableStateOf(false)
        private set

    var userException by mutableStateOf<CommonException?>(null)
        private set

    val tileContext = IosArTileContext(
        getTilesUseCase = getTilesUseCase,
        buildArTileTextureUseCase = buildArTileTextureUseCase,
        onError = ::postUserMessage
    )

    private val floorArController = FloorArController(
        onStateChanged = { contourState = it }
    )

    val coordinator = IosArSessionCoordinator(
        floorArController = floorArController,
        onTrackingNameChanged = { trackingStateName = it },
        onPlaneDebugMetricsChanged = { planeDebugMetrics = it },
        onPlacementHintChanged = { placementHint = it },
        onContourRealignAvailableChanged = { showContourRealignButton = it }
    )

    fun loadInitialSelection(tileId: Long?, layoutId: String?, paletteId: String?) {
        tileContext.setInitialSelection(tileId, layoutId, paletteId)
        scope.launch {
            tileContext.loadCatalog()
            syncTileTextureToRenderer()
            refreshTileUi()
        }
    }

    fun openTilePicker() {
        tileContext.openPicker()
        compactHint = null
        refreshTileUi()
    }

    fun closeTilePicker() {
        tileContext.closePicker()
        refreshTileUi()
    }

    fun onPickerTileSelected(tileId: Long) {
        tileContext.onPickerTileSelected(
            tileId = tileId,
            isTileVisible = contourState.isTileVisible,
            onDeselect = ::deselectTile,
            onApply = ::applyTileToContour
        )
        syncTileTextureToRenderer()
        refreshTileUi()
    }

    fun retryCatalogLoad() {
        scope.launch {
            tileContext.retryCatalogLoad()
            refreshTileUi()
        }
    }

    fun deselectTile() {
        removeTileFill()
        tileContext.clearTileSelection()
        syncTileTextureToRenderer()
        refreshTileUi()
    }

    fun onPickerLayoutSelected(layoutId: String) {
        tileContext.onPickerLayoutSelected(layoutId, ::applyTileToContour)
        syncTileTextureToRenderer()
        refreshTileUi()
    }

    fun onPickerPaletteSelected(paletteId: String) {
        tileContext.onPickerPaletteSelected(paletteId, ::applyTileToContour)
        syncTileTextureToRenderer()
        refreshTileUi()
    }

    fun toggleTileVisibility() {
        if (contourState.isTileVisible) {
            openTilePicker()
            return
        }
        if (tileContext.arTileTexture != null) {
            applyTileToContour()
            compactHint = "Плитка наложена"
            return
        }
        openTilePicker()
    }

    fun removeTileFill() {
        if (contourState.isTileVisible) {
            floorArController.onEvent(com.example.arplitka.shared.ar.contracts.state.FloorArEvent.ToggleTileVisibility)
        }
        compactHint = "Плитка убрана"
    }

    fun applyTileToContour() {
        if (!contourState.isFinalized) return
        if (!contourState.isTileVisible) {
            floorArController.onEvent(com.example.arplitka.shared.ar.contracts.state.FloorArEvent.ToggleTileVisibility)
        }
        syncTileTextureToRenderer()
        tileContext.closePicker()
        compactHint = null
    }

    fun onContourConfirmedWithAutoApply() {
        if (tileContext.pendingAutoApply && tileContext.arTileTexture != null) {
            tileContext.pendingAutoApply = false
            applyTileToContour()
            compactHint = "Плитка наложена"
        } else if (contourState.isFinalized) {
            compactHint = "Добавьте плитку на зону"
        }
    }

    fun toggleDebugPanel() {
        showDebugPanel = !showDebugPanel
    }

    fun syncTileTextureToRenderer() {
        coordinator.setExternalTileTexture(tileContext.arTileTexture)
    }

    fun clearUserMessage() {
        userException = null
    }

    private fun postUserMessage(exception: CommonException) {
        userException = exception
    }

    fun pause() {
        coordinator.pause()
    }

    var tilePickerState by mutableStateOf(ArTilePickerState())
        private set

    var colorRailPalettesState by mutableStateOf<List<com.example.arplitka.shared.ui.kit.ar.ArPickerPaletteUi>>(emptyList())
        private set

    val selectedTileName: String?
        get() = tileContext.selectedTileName

    fun refreshTileUi() {
        tilePickerState = tileContext.tilePicker
        colorRailPalettesState = tileContext.colorRailPalettes
    }
}
