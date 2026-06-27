package com.example.arplitka.features.floordetection.presentation.viewmodel

import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arplitka.features.floordetection.domain.logic.ArTileSelectionResolver
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
import com.example.arplitka.shared.core.domain.model.CustomResultModelDomain
import com.example.arplitka.shared.core.domain.presentation.SingleFlowEvent
import com.example.arplitka.shared.tiles.domain.model.Tile
import com.example.arplitka.shared.tiles.domain.model.TileSelection
import com.example.arplitka.shared.tiles.domain.usecase.BuildArTileTextureUseCase
import com.example.arplitka.shared.tiles.domain.usecase.GetTileByIdUseCase
import com.example.arplitka.shared.tiles.domain.usecase.GetTilesUseCase
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

class FloorArViewModel(
    private val processArFrameUseCase: ProcessArFrameUseCase,
    private val getTileByIdUseCase: GetTileByIdUseCase,
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

    private var loadedTile: Tile? = null

    init {
        if (initialTileId != null) {
            viewModelScope.launch {
                loadInitialTile(
                    tileId = initialTileId,
                    layoutId = initialLayoutId,
                    paletteId = initialPaletteId,
                    autoApplyOnConfirm = true
                )
            }
        }
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
                val applied = applyTileTexture(state)
                return@update applied.copy(
                    isContourConfirmed = true,
                    snappedPointIndex = null,
                    pendingAutoApplyTile = false
                )
            }

            val newState = state.copy(
                isContourConfirmed = true,
                isTileVisible = false,
                snappedPointIndex = null,
                stage = FloorWorkflowStage.CONTOUR_CONFIRMED
            )
            applyContourPhaseUi(newState)
        }
    }

    fun toggleTileVisibility() {
        val state = _uiState.value
        if (state.stage.ordinal < FloorWorkflowStage.CONTOUR_CONFIRMED.ordinal) return

        if (state.isTileVisible) {
            _uiState.update { current ->
                applyContourPhaseUi(
                    current.copy(
                        isTileVisible = false,
                        stage = FloorWorkflowStage.CONTOUR_CONFIRMED
                    )
                )
            }
            return
        }

        if (state.arTileTexture != null) {
            _uiState.update { applyTileTexture(it) }
            return
        }

        viewModelScope.launch { applyDefaultCatalogTile() }
    }

    fun clearSection() {
        _uiState.update { state ->
            state.points.forEach { point -> point.anchor.detach() }
            val newState = FloorUiState(
                trackingState = state.trackingState,
                horizontalPlaneCount = state.horizontalPlaneCount,
                selectedArea = state.selectedArea,
                hasCenterHit = state.hasCenterHit,
                isDepthEnabled = state.isDepthEnabled,
                status = state.status,
                instruction = state.instruction,
                detectionState = state.detectionState,
                tileSelection = state.tileSelection,
                arTileTexture = state.arTileTexture,
                selectedTileName = state.selectedTileName,
                pendingAutoApplyTile = state.pendingAutoApplyTile
            )
            val nextStage = calculateNextStage(newState)
            applyContourPhaseUi(newState.copy(stage = nextStage))
        }
    }

    fun rotateTexture() {
        _uiState.update { state ->
            if (!state.isTileVisible) return@update state
            val nextRotation = TextureRotation.entries[(state.textureRotation.ordinal + 1) % TextureRotation.entries.size]
            val rotationDegrees = nextRotation.ordinal * 45f
            val tile = loadedTile
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
        _uiState.update { state ->
            if (!state.isContourConfirmed || !state.isTileVisible) return@update state

            val tile = loadedTile
            val selection = state.tileSelection
            if (tile == null || selection == null) {
                val nextOrdinal = (state.selectedTileType.ordinal + 1) % TileType.entries.size
                return@update state.copy(selectedTileType = TileType.entries[nextOrdinal])
            }

            val layout = tile.layouts.find { it.id == selection.layoutId } ?: tile.layouts.firstOrNull()
            val palettes = layout?.palettes.orEmpty()
            if (palettes.size <= 1) {
                val nextOrdinal = (state.selectedTileType.ordinal + 1) % TileType.entries.size
                return@update state.copy(selectedTileType = TileType.entries[nextOrdinal])
            }

            val currentIndex = palettes.indexOfFirst { it.id == selection.paletteId }.coerceAtLeast(0)
            val nextPalette = palettes[(currentIndex + 1) % palettes.size]
            val nextSelection = selection.copy(
                paletteId = nextPalette.id,
                selectedColorsBySlot = nextPalette.selectedColorsBySlot
            )
            val nextTexture = ArTileSelectionResolver.buildTexture(
                tile = tile,
                selection = nextSelection,
                rotationDegrees = state.textureRotation.ordinal * 45f,
                buildArTileTextureUseCase = buildArTileTextureUseCase
            ) ?: return@update state

            state.copy(
                tileSelection = nextSelection,
                arTileTexture = nextTexture
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
                pendingAutoApplyTile = state.pendingAutoApplyTile
            )
        }
    }

    fun onBack() {
        val returnToTileId = loadedTile?.id ?: _uiState.value.tileSelection?.tileId
        _event.emit(FloorArEvent.NavigateBack(returnToTileId = returnToTileId))
    }

    override fun onCleared() {
        _uiState.value.points.forEach { point -> point.anchor.detach() }
        super.onCleared()
    }

    private suspend fun loadInitialTile(
        tileId: Long,
        layoutId: String?,
        paletteId: String?,
        autoApplyOnConfirm: Boolean
    ) {
        when (val result = getTileByIdUseCase(tileId)) {
            is CustomResultModelDomain.Success -> applyLoadedTile(
                tile = result.result,
                layoutId = layoutId,
                paletteId = paletteId,
                autoApplyOnConfirm = autoApplyOnConfirm
            )
            is CustomResultModelDomain.Error -> Unit
        }
    }

    private suspend fun applyDefaultCatalogTile() {
        when (val result = getTilesUseCase()) {
            is CustomResultModelDomain.Success -> {
                val tile = result.result.firstOrNull() ?: return
                applyLoadedTile(
                    tile = tile,
                    layoutId = null,
                    paletteId = null,
                    autoApplyOnConfirm = false,
                    applyImmediately = true
                )
            }
            is CustomResultModelDomain.Error -> Unit
        }
    }

    private fun applyLoadedTile(
        tile: Tile,
        layoutId: String?,
        paletteId: String?,
        autoApplyOnConfirm: Boolean,
        applyImmediately: Boolean = false
    ) {
        loadedTile = tile
        val selection = ArTileSelectionResolver.resolveSelection(tile, layoutId, paletteId)
        val texture = ArTileSelectionResolver.buildTexture(
            tile = tile,
            selection = selection,
            rotationDegrees = _uiState.value.textureRotation.ordinal * 45f,
            buildArTileTextureUseCase = buildArTileTextureUseCase
        ) ?: return

        _uiState.update { state ->
            var updated = state.copy(
                tileSelection = selection,
                arTileTexture = texture,
                selectedTileName = tile.name,
                pendingAutoApplyTile = autoApplyOnConfirm && !applyImmediately
            )
            if (applyImmediately && state.stage.ordinal >= FloorWorkflowStage.CONTOUR_CONFIRMED.ordinal) {
                updated = applyTileTexture(updated)
            }
            updated
        }
    }

    private fun applyTileTexture(state: FloorUiState): FloorUiState {
        val applied = state.copy(
            isTileVisible = true,
            isContourConfirmed = true,
            stage = FloorWorkflowStage.TILE_LAYOUT
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
