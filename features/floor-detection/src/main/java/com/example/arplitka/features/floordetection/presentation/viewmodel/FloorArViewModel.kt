package com.example.arplitka.features.floordetection.presentation.viewmodel

import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arplitka.shared.tiles.domain.logic.ArTileSelectionResolver
import com.example.arplitka.shared.ui.core.model.toUiModel
import com.example.arplitka.shared.ui.kit.ar.buildArColorRailPalettes
import com.example.arplitka.shared.ui.kit.ar.buildArTilePickerState
import com.example.arplitka.features.floordetection.domain.model.ArPoint
import com.example.arplitka.features.floordetection.domain.model.FloorDetectionState
import com.example.arplitka.features.floordetection.domain.model.FloorUiState
import com.example.arplitka.features.floordetection.domain.model.TextureRotation
import com.example.arplitka.features.floordetection.domain.model.TileType
import com.example.arplitka.features.floordetection.domain.usecase.ProcessArFrameUseCase
import com.example.arplitka.features.floordetection.presentation.FloorArEvent
import com.example.arplitka.shared.ar.contracts.model.ArInstruction
import com.example.arplitka.shared.ar.contracts.model.ArTrackingStatus
import com.example.arplitka.shared.ar.domain.model.FloorWorkflowStage
import com.example.arplitka.shared.core.domain.model.CommonException
import com.example.arplitka.shared.core.domain.model.CustomResultModelDomain
import com.example.arplitka.shared.core.domain.presentation.SingleFlowEvent
import com.example.arplitka.shared.tiles.domain.model.ArCatalogState
import com.example.arplitka.shared.tiles.domain.model.Tile
import com.example.arplitka.shared.tiles.domain.model.TileSelection
import com.example.arplitka.shared.tiles.domain.usecase.BuildArTileTextureUseCase
import com.example.arplitka.shared.tiles.domain.usecase.GetTilesUseCase
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.example.arplitka.shared.ui.kit.ar.ArTilePickerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

class FloorArViewModel(
    private val processArFrameUseCase: ProcessArFrameUseCase,
    private val getTilesUseCase: GetTilesUseCase,
    private val buildArTileTextureUseCase: BuildArTileTextureUseCase,
    private val initialTileId: Long?,
    private val initialLayoutId: String?,
    private val initialPaletteId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(FloorUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = SingleFlowEvent<FloorArEvent>(viewModelScope)
    val event = _event.flow

    private var initialSelectionHandled = false

    init {
        viewModelScope.launch { loadCatalog() }
    }

    fun onSessionUpdated(session: Session, frame: Frame, viewportSize: IntSize) {
        val result = processArFrameUseCase(session, frame, viewportSize)

        _uiState.update { currentState ->
            val updatedPoints = currentState.points.map { point ->
                point.copy(pose = point.anchor.pose)
            }

            var newState = if (result.trackingState != TrackingState.TRACKING) {
                currentState.copy(
                    trackingState = result.trackingState,
                    horizontalPlaneCount = result.horizontalPlaneCount,
                    hasCenterHit = false,
                    isDepthEnabled = result.isDepthEnabled,
                    status = ArTrackingStatus.TRACKING_LOST,
                    instruction = ArInstruction.MOVE_PHONE,
                    currentHitPose = null,
                    currentHitResult = null,
                    snappedPointIndex = null,
                    points = updatedPoints
                )
            } else if (!result.isFloorDetected) {
                currentState.copy(
                    detectionState = FloorDetectionState.SearchingFloor,
                    trackingState = result.trackingState,
                    horizontalPlaneCount = result.horizontalPlaneCount,
                    selectedArea = result.selectedArea,
                    hasCenterHit = result.hasCenterHit,
                    isDepthEnabled = result.isDepthEnabled,
                    status = ArTrackingStatus.SEARCHING_FLOOR,
                    instruction = ArInstruction.SEARCHING,
                    currentHitPose = result.hitPose,
                    currentHitResult = result.hitResult,
                    snappedPointIndex = null,
                    points = updatedPoints
                )
            } else {
                currentState.copy(
                    detectionState = FloorDetectionState.CandidateFound,
                    trackingState = result.trackingState,
                    horizontalPlaneCount = result.horizontalPlaneCount,
                    selectedArea = result.selectedArea,
                    hasCenterHit = true,
                    isDepthEnabled = result.isDepthEnabled,
                    status = if (currentState.isPolygonClosed) ArTrackingStatus.POLYGON_CLOSED else ArTrackingStatus.FLOOR_DETECTED,
                    instruction = ArInstruction.DETECTED,
                    currentHitPose = result.hitPose,
                    currentHitResult = result.hitResult,
                    points = updatedPoints
                )
            }

            if (newState.stage.ordinal < FloorWorkflowStage.CONTOUR_CONFIRMED.ordinal && newState.hasCenterHit && newState.currentHitPose != null && newState.points.isNotEmpty()) {
                val currentPose = newState.currentHitPose!!
                val firstPoint = newState.points.first().pose
                val distToFirst = calculateDistance(
                    firstPoint.tx(), firstPoint.ty(), firstPoint.tz(),
                    currentPose.tx(), currentPose.ty(), currentPose.tz()
                )

                if (newState.points.size >= 3 && distToFirst < CLOSE_THRESHOLD_M) {
                    newState = newState.copy(
                        isPolygonClosed = true,
                        snappedPointIndex = 0
                    )
                } else if (distToFirst < SNAP_THRESHOLD_M) {
                    newState = newState.copy(
                        snappedPointIndex = 0,
                        isPolygonClosed = false
                    )
                } else {
                    var foundSnap = false
                    for (i in 1 until newState.points.size) {
                        val p = newState.points[i].pose
                        val dist = calculateDistance(
                            p.tx(), p.ty(), p.tz(),
                            currentPose.tx(), currentPose.ty(), currentPose.tz()
                        )
                        if (dist < SNAP_THRESHOLD_M) {
                            newState = newState.copy(snappedPointIndex = i)
                            foundSnap = true
                            break
                        }
                    }
                    if (!foundSnap) {
                        newState = newState.copy(
                            snappedPointIndex = null,
                            isPolygonClosed = false
                        )
                    }
                }
            } else if (newState.stage.ordinal < FloorWorkflowStage.CONTOUR_CONFIRMED.ordinal) {
                newState = newState.copy(
                    snappedPointIndex = null,
                    isPolygonClosed = false
                )
            }

            val nextStage = calculateNextStage(newState)
            applyContourPhaseUi(newState.copy(stage = nextStage))
        }
    }

    private fun calculateNextStage(state: FloorUiState): FloorWorkflowStage {
        if (state.stage == FloorWorkflowStage.TILE_LAYOUT) return FloorWorkflowStage.TILE_LAYOUT
        if (state.stage == FloorWorkflowStage.CONTOUR_CONFIRMED) {
            return if (state.isTileVisible) FloorWorkflowStage.TILE_LAYOUT else FloorWorkflowStage.CONTOUR_CONFIRMED
        }

        if (state.trackingState != TrackingState.TRACKING) return FloorWorkflowStage.INITIALIZING
        if (state.detectionState == FloorDetectionState.SearchingFloor) return FloorWorkflowStage.SEARCHING_FLOOR

        return when {
            state.isContourConfirmed -> FloorWorkflowStage.CONTOUR_CONFIRMED
            state.isPolygonClosed -> FloorWorkflowStage.CONTOUR_CLOSED
            state.points.isEmpty() -> FloorWorkflowStage.PLACEMENT_EMPTY
            else -> FloorWorkflowStage.PLACEMENT_ACTIVE
        }
    }

    private fun applyContourPhaseUi(state: FloorUiState): FloorUiState = when (state.stage) {
        FloorWorkflowStage.TILE_LAYOUT -> state.copy(
            status = ArTrackingStatus.POLYGON_CLOSED,
            instruction = ArInstruction.TILE_VISIBLE
        )
        FloorWorkflowStage.CONTOUR_CONFIRMED -> state.copy(
            status = ArTrackingStatus.POLYGON_CLOSED,
            instruction = ArInstruction.CONTOUR_CONFIRMED
        )
        FloorWorkflowStage.CONTOUR_CLOSED -> state.copy(
            status = ArTrackingStatus.POLYGON_CLOSED,
            instruction = ArInstruction.CONTOUR_CLOSED
        )
        else -> state
    }

    fun addPoint() {
        _uiState.update { state ->
            if (state.stage.ordinal >= FloorWorkflowStage.CONTOUR_CLOSED.ordinal) return@update state
            if (state.snappedPointIndex != null) return@update state

            val hitResult = state.currentHitResult ?: return@update state
            val poseToAdd = hitResult.hitPose

            val lastPose = state.points.lastOrNull()?.pose
            if (lastPose != null) {
                val distanceToLast = calculateDistance(
                    lastPose.tx(), lastPose.ty(), lastPose.tz(),
                    poseToAdd.tx(), poseToAdd.ty(), poseToAdd.tz()
                )
                if (distanceToLast < SNAP_THRESHOLD_M) return@update state
            }

            val floorY = state.points.firstOrNull()?.pose?.ty()
            if (floorY != null && abs(poseToAdd.ty() - floorY) > MAX_POINT_HEIGHT_DELTA_M) return@update state

            val anchor = runCatching { hitResult.createAnchor() }.getOrNull() ?: return@update state

            val newState = state.copy(points = state.points + ArPoint(anchor, anchor.pose))
            val nextStage = calculateNextStage(newState)
            applyContourPhaseUi(newState.copy(stage = nextStage))
        }
    }

    fun undoPoint() {
        _uiState.update { state ->
            if (state.stage.ordinal >= FloorWorkflowStage.CONTOUR_CONFIRMED.ordinal) return@update state
            if (state.points.isNotEmpty()) {
                state.points.last().anchor.detach()
                val newState = state.copy(
                    points = state.points.dropLast(1),
                    isPolygonClosed = false
                )
                val nextStage = calculateNextStage(newState)
                applyContourPhaseUi(newState.copy(stage = nextStage))
            } else {
                state
            }
        }
    }

    fun confirmContour() {
        _uiState.update { state ->
            if (state.stage != FloorWorkflowStage.CONTOUR_CLOSED) return@update state

            if (state.pendingAutoApplyTile && state.arTileTexture != null) {
                beginTileApply()
                val applied = applyTileTexture(state)
                return@update applied.copy(
                    isContourConfirmed = true,
                    snappedPointIndex = null,
                    pendingAutoApplyTile = false,
                    compactHint = "Плитка наложена",
                    colorRailPalettes = buildArColorRailPalettes(state.selectedTile, applied.tileSelection)
                )
            }

            val newState = state.copy(
                isContourConfirmed = true,
                isTileVisible = false,
                snappedPointIndex = null,
                stage = FloorWorkflowStage.CONTOUR_CONFIRMED,
                compactHint = "Добавьте плитку на зону"
            )
            applyContourPhaseUi(newState)
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userException = null) }
    }

    private fun postUserMessage(exception: CommonException) {
        _uiState.update { it.copy(userException = exception) }
    }

    fun openTilePicker() {
        _uiState.update { state ->
            state.copy(
                tilePicker = buildPickerState(state = state, isVisible = true),
                compactHint = null
            )
        }
    }

    fun retryCatalogLoad() {
        viewModelScope.launch {
            _uiState.update { it.copy(catalogState = ArCatalogState.Loading) }
            when (val result = getTilesUseCase()) {
                is CustomResultModelDomain.Success -> {
                    val tiles = result.result
                    _uiState.update { it.copy(catalogState = ArCatalogState.Content(tiles)) }
                    tryApplyInitialSelection(tiles)
                    refreshPickerState()
                }
                is CustomResultModelDomain.Error -> {
                    _uiState.update { it.copy(catalogState = ArCatalogState.Error(result.exception)) }
                    refreshPickerState()
                }
            }
        }
    }

    fun closeTilePicker() {
        _uiState.update { it.copy(tilePicker = it.tilePicker.copy(isVisible = false)) }
    }

    fun onPickerTileSelected(tileId: Long) {
        val state = _uiState.value
        if (state.isTileApplying) return
        if (state.tileSelection?.tileId == tileId) {
            deselectTile()
            return
        }

        val tiles = (state.catalogState as? ArCatalogState.Content)?.tiles
        val tile = tiles?.find { it.id == tileId }
        if (tile == null) {
            postUserMessage(CommonException.Client)
            return
        }

        val applyImmediately = state.stage.ordinal >= FloorWorkflowStage.CONTOUR_CONFIRMED.ordinal
        if (applyImmediately) beginTileApply()

        applySelectedTile(
            tile = tile,
            layoutId = null,
            paletteId = null,
            autoApplyOnConfirm = false,
            applyImmediately = applyImmediately
        )
        refreshPickerState()
    }

    fun onPickerLayoutSelected(layoutId: String) {
        if (_uiState.value.isTileApplying) return
        val tile = _uiState.value.selectedTile ?: return
        val selection = _uiState.value.tileSelection ?: return
        if (selection.layoutId == layoutId) return

        val layout = tile.layouts.find { it.id == layoutId } ?: return
        val palette = layout.palettes.firstOrNull() ?: return
        val applyImmediately = _uiState.value.stage.ordinal >= FloorWorkflowStage.CONTOUR_CONFIRMED.ordinal
        if (applyImmediately) beginTileApply()

        applySelection(
            tile = tile,
            selection = TileSelection(
                tileId = tile.id,
                layoutId = layout.id,
                paletteId = palette.id,
                selectedColorsBySlot = palette.selectedColorsBySlot
            ),
            applyImmediately = applyImmediately
        )
        refreshPickerState()
    }

    fun onPickerPaletteSelected(paletteId: String) {
        if (_uiState.value.isTileApplying) return
        val tile = _uiState.value.selectedTile ?: return
        val selection = _uiState.value.tileSelection ?: return
        if (selection.paletteId == paletteId) return

        val layout = tile.layouts.find { it.id == selection.layoutId } ?: tile.layouts.firstOrNull() ?: return
        val palette = layout.palettes.find { it.id == paletteId } ?: return
        val applyImmediately = _uiState.value.isTileVisible
        if (applyImmediately) beginTileApply()

        applySelection(
            tile = tile,
            selection = selection.copy(
                paletteId = palette.id,
                selectedColorsBySlot = palette.selectedColorsBySlot
            ),
            applyImmediately = applyImmediately
        )
        refreshPickerState()
    }

    fun onColorRailPaletteSelected(paletteId: String) {
        onPickerPaletteSelected(paletteId)
    }

    fun toggleDebugPanel() {
        _uiState.update { it.copy(showDebugPanel = !it.showDebugPanel) }
    }

    private fun removeTileFill() {
        _uiState.update { state ->
            applyContourPhaseUi(
                state.copy(
                    isTileVisible = false,
                    stage = FloorWorkflowStage.CONTOUR_CONFIRMED
                )
            )
        }
    }

    private fun deselectTile() {
        _uiState.update { state ->
            applyContourPhaseUi(
                state.copy(
                    tileSelection = null,
                    selectedTile = null,
                    arTileTexture = null,
                    selectedTileName = null,
                    colorRailPalettes = emptyList(),
                    pendingAutoApplyTile = false,
                    compactHint = "Плитка убрана",
                    isTileVisible = false,
                    isTileApplying = false,
                    stage = FloorWorkflowStage.CONTOUR_CONFIRMED,
                    tilePicker = buildPickerState(
                        state = state,
                        isVisible = state.tilePicker.isVisible,
                        selectedTile = null,
                        selection = null
                    )
                )
            )
        }
    }

    private suspend fun loadCatalog() {
        val current = _uiState.value.catalogState
        if (current is ArCatalogState.Content) {
            refreshPickerState()
            return
        }
        if (current is ArCatalogState.Loading) return

        _uiState.update { it.copy(catalogState = ArCatalogState.Loading) }
        when (val result = getTilesUseCase()) {
            is CustomResultModelDomain.Success -> {
                val tiles = result.result
                _uiState.update { it.copy(catalogState = ArCatalogState.Content(tiles)) }
                tryApplyInitialSelection(tiles)
                refreshPickerState()
            }
            is CustomResultModelDomain.Error -> {
                _uiState.update { it.copy(catalogState = ArCatalogState.Error(result.exception)) }
                refreshPickerState()
            }
        }
    }

    private fun tryApplyInitialSelection(tiles: List<Tile>) {
        if (initialSelectionHandled) return
        initialSelectionHandled = true
        val tileId = initialTileId ?: return

        val tile = tiles.find { it.id == tileId }
        if (tile == null) {
            postUserMessage(CommonException.Client)
            return
        }
        applySelectedTile(
            tile = tile,
            layoutId = initialLayoutId,
            paletteId = initialPaletteId,
            autoApplyOnConfirm = true
        )
    }

    private fun buildPickerState(
        state: FloorUiState,
        isVisible: Boolean,
        selectedTile: Tile? = state.selectedTile,
        selection: TileSelection? = state.tileSelection
    ): ArTilePickerState {
        return when (val catalog = state.catalogState) {
            ArCatalogState.Initial, ArCatalogState.Loading -> ArTilePickerState(
                isVisible = isVisible,
                isCatalogLoading = true
            )
            is ArCatalogState.Error -> ArTilePickerState(
                isVisible = isVisible,
                catalogLoadError = catalog.exception.toUiModel()
            )
            is ArCatalogState.Content -> buildArTilePickerState(
                tiles = catalog.tiles,
                selectedTile = selectedTile,
                selection = selection,
                isVisible = isVisible
            )
        }
    }

    private fun refreshPickerState() {
        _uiState.update { state ->
            state.copy(
                tilePicker = buildPickerState(state = state, isVisible = state.tilePicker.isVisible),
                colorRailPalettes = buildArColorRailPalettes(state.selectedTile, state.tileSelection)
            )
        }
    }

    private fun applySelection(
        tile: Tile,
        selection: TileSelection,
        applyImmediately: Boolean
    ) {
        val texture = ArTileSelectionResolver.buildTexture(
            tile = tile,
            selection = selection,
            rotationDegrees = _uiState.value.textureRotation.ordinal * 45f,
            buildArTileTextureUseCase = buildArTileTextureUseCase
        ) ?: return

        _uiState.update { state ->
            var updated = state.copy(
                selectedTile = tile,
                tileSelection = selection,
                arTileTexture = texture,
                selectedTileName = tile.name,
                colorRailPalettes = buildArColorRailPalettes(tile, selection)
            )
            if (applyImmediately) {
                updated = applyTileTexture(updated)
            }
            updated
        }
    }

    fun clearSection() {
        _uiState.update { state ->
            state.points.forEach { point -> point.anchor.detach() }
            val baseState = FloorUiState(
                trackingState = state.trackingState,
                horizontalPlaneCount = state.horizontalPlaneCount,
                selectedArea = state.selectedArea,
                hasCenterHit = state.hasCenterHit,
                isDepthEnabled = state.isDepthEnabled,
                status = state.status,
                instruction = state.instruction,
                detectionState = state.detectionState,
                catalogState = state.catalogState,
                showDebugPanel = state.showDebugPanel,
                arSessionResetKey = state.arSessionResetKey + 1
            )
            val nextStage = calculateNextStage(baseState)
            val stagedState = baseState.copy(stage = nextStage)
            applyContourPhaseUi(
                stagedState.copy(
                    tilePicker = buildPickerState(
                        state = stagedState,
                        isVisible = state.tilePicker.isVisible,
                        selectedTile = null,
                        selection = null
                    )
                )
            )
        }
    }

    fun rotateTexture() {
        _uiState.update { state ->
            if (!state.isTileVisible) return@update state
            val nextRotation = TextureRotation.entries[(state.textureRotation.ordinal + 1) % TextureRotation.entries.size]
            val rotationDegrees = nextRotation.ordinal * 45f
            val tile = state.selectedTile
            val selection = state.tileSelection

            val updatedTexture = if (tile != null && selection != null) {
                ArTileSelectionResolver.buildTexture(
                    tile = tile,
                    selection = selection,
                    rotationDegrees = rotationDegrees,
                    buildArTileTextureUseCase = buildArTileTextureUseCase
                )
            } else {
                state.arTileTexture?.copy(rotationDegrees = rotationDegrees)
            }

            state.copy(
                textureRotation = nextRotation,
                arTileTexture = updatedTexture ?: state.arTileTexture
            )
        }
    }

    fun changeTileType() {
        openTilePicker()
    }

    fun toggleTileVisibility() {
        val state = _uiState.value
        if (state.stage.ordinal < FloorWorkflowStage.CONTOUR_CONFIRMED.ordinal) return
        if (state.isTileApplying) return

        if (state.isTileVisible) {
            removeTileFill()
            return
        }

        if (state.arTileTexture != null) {
            beginTileApply()
            _uiState.update {
                applyTileTexture(it).copy(
                    compactHint = "Плитка наложена"
                )
            }
            return
        }

        openTilePicker()
    }

    fun onTileTextureApplied(requestKey: Int) {
        val state = _uiState.value
        if (!state.isTileApplying || state.tileApplyRequestKey != requestKey) return
        _uiState.update { it.copy(isTileApplying = false) }
    }

    private fun beginTileApply() {
        _uiState.update {
            it.copy(
                isTileApplying = true,
                tileApplyRequestKey = it.tileApplyRequestKey + 1
            )
        }
    }

    fun reset() {
        _uiState.update { state ->
            state.points.forEach { point -> point.anchor.detach() }
            FloorUiState(
                tileSelection = state.tileSelection,
                arTileTexture = state.arTileTexture,
                selectedTileName = state.selectedTileName,
                pendingAutoApplyTile = state.pendingAutoApplyTile,
                colorRailPalettes = state.colorRailPalettes,
                catalogState = state.catalogState,
                selectedTile = state.selectedTile
            )
        }
    }

    fun onBack() {
        val returnToTileId = _uiState.value.tileSelection?.tileId
        _event.emit(FloorArEvent.NavigateBack(returnToTileId = returnToTileId))
    }

    override fun onCleared() {
        _uiState.value.points.forEach { point -> point.anchor.detach() }
        super.onCleared()
    }

    private fun applySelectedTile(
        tile: Tile,
        layoutId: String?,
        paletteId: String?,
        autoApplyOnConfirm: Boolean,
        applyImmediately: Boolean = false
    ) {
        val selection = ArTileSelectionResolver.resolveSelection(tile, layoutId, paletteId)
        val texture = ArTileSelectionResolver.buildTexture(
            tile = tile,
            selection = selection,
            rotationDegrees = _uiState.value.textureRotation.ordinal * 45f,
            buildArTileTextureUseCase = buildArTileTextureUseCase
        )

        _uiState.update { state ->
            var updated = state.copy(
                selectedTile = tile,
                tileSelection = selection,
                arTileTexture = texture,
                selectedTileName = tile.name,
                colorRailPalettes = buildArColorRailPalettes(tile, selection),
                pendingAutoApplyTile = autoApplyOnConfirm && !applyImmediately
            )
            if (applyImmediately && texture != null &&
                state.stage.ordinal >= FloorWorkflowStage.CONTOUR_CONFIRMED.ordinal
            ) {
                updated = applyTileTexture(updated)
            }
            updated
        }

        if (texture == null) {
            postUserMessage(CommonException.Serialization)
        }
    }

    private fun applyTileTexture(state: FloorUiState): FloorUiState {
        val applied = state.copy(
            isTileVisible = true,
            isContourConfirmed = true,
            stage = FloorWorkflowStage.TILE_LAYOUT,
            compactHint = null,
            tilePicker = state.tilePicker.copy(isVisible = false)
        )
        return applyContourPhaseUi(applied)
    }

    private fun calculateDistance(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float {
        return sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2) + (z1 - z2) * (z1 - z2))
    }

    companion object {
        private const val CLOSE_THRESHOLD_M = 0.10f
        private const val SNAP_THRESHOLD_M = 0.02f
        private const val MAX_POINT_HEIGHT_DELTA_M = 0.08f
    }
}
