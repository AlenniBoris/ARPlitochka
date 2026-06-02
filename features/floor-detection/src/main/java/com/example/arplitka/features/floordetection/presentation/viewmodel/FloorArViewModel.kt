package com.example.arplitka.features.floordetection.presentation.viewmodel

import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import com.example.arplitka.shared.ar.contracts.model.ArInstruction
import com.example.arplitka.shared.ar.contracts.model.ArTrackingStatus
import com.example.arplitka.features.floordetection.domain.model.ArPoint
import com.example.arplitka.features.floordetection.domain.model.FloorDetectionState
import com.example.arplitka.features.floordetection.domain.model.FloorUiState
import com.example.arplitka.features.floordetection.domain.model.TextureRotation
import com.example.arplitka.features.floordetection.domain.model.TileType
import com.example.arplitka.features.floordetection.domain.usecase.ProcessArFrameUseCase
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.sqrt

@HiltViewModel
class FloorArViewModel @Inject constructor(
    private val processArFrameUseCase: ProcessArFrameUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FloorUiState())
    val uiState = _uiState.asStateFlow()
    
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
            
            if (!newState.isContourConfirmed && !newState.isTileVisible && newState.hasCenterHit && newState.currentHitPose != null && newState.points.isNotEmpty()) {
                val currentPose = newState.currentHitPose!!
                val firstPoint = newState.points.first().pose
                val distToFirst = calculateDistance(firstPoint.tx(), firstPoint.ty(), firstPoint.tz(), 
                                                   currentPose.tx(), currentPose.ty(), currentPose.tz())
                
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
                        val dist = calculateDistance(p.tx(), p.ty(), p.tz(), 
                                                     currentPose.tx(), currentPose.ty(), currentPose.tz())
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
            } else if (!newState.isContourConfirmed && !newState.isTileVisible) {
                newState = newState.copy(snappedPointIndex = null)
            }

            applyContourPhaseUi(newState)
        }
    }

    private fun applyContourPhaseUi(state: FloorUiState): FloorUiState = when {
        state.isContourConfirmed && state.isTileVisible -> state.copy(
            status = ArTrackingStatus.POLYGON_CLOSED,
            instruction = ArInstruction.TILE_VISIBLE
        )
        state.isContourConfirmed -> state.copy(
            status = ArTrackingStatus.POLYGON_CLOSED,
            instruction = ArInstruction.CONTOUR_CONFIRMED
        )
        state.isPolygonClosed -> state.copy(
            status = ArTrackingStatus.POLYGON_CLOSED,
            instruction = ArInstruction.CONTOUR_CLOSED
        )
        else -> state
    }
    
    fun addPoint() {
        _uiState.update { state ->
            if (state.isPolygonClosed || state.isContourConfirmed || state.isTileVisible) return@update state

            if (state.snappedPointIndex != null) {
                return@update state
            }

            val hitResult = state.currentHitResult ?: return@update state
            val poseToAdd = hitResult.hitPose

            val lastPose = state.points.lastOrNull()?.pose
            if (lastPose != null) {
                val distanceToLast = calculateDistance(
                    lastPose.tx(),
                    lastPose.ty(),
                    lastPose.tz(),
                    poseToAdd.tx(),
                    poseToAdd.ty(),
                    poseToAdd.tz()
                )
                if (distanceToLast < SNAP_THRESHOLD_M) {
                    return@update state
                }
            }

            val floorY = state.points.firstOrNull()?.pose?.ty()
            if (floorY != null && abs(poseToAdd.ty() - floorY) > MAX_POINT_HEIGHT_DELTA_M) {
                return@update state
            }

            val anchor = runCatching { hitResult.createAnchor() }.getOrNull()
                ?: return@update state
            
            state.copy(points = state.points + ArPoint(anchor, anchor.pose))
        }
    }
    
    fun undoPoint() {
        _uiState.update { state ->
            if (state.isContourConfirmed || state.isTileVisible) return@update state
            if (state.points.isNotEmpty()) {
                state.points.last().anchor.detach()
                applyContourPhaseUi(
                    state.copy(
                        points = state.points.dropLast(1),
                        isPolygonClosed = false
                    )
                )
            } else {
                state
            }
        }
    }

    fun confirmContour() {
        _uiState.update { state ->
            if (!state.isPolygonClosed || state.isContourConfirmed) return@update state
            applyContourPhaseUi(
                state.copy(
                    isContourConfirmed = true,
                    isTileVisible = false,
                    snappedPointIndex = null
                )
            )
        }
    }

    fun toggleTileVisibility() {
        _uiState.update { state ->
            if (!state.isContourConfirmed) return@update state
            applyContourPhaseUi(state.copy(isTileVisible = !state.isTileVisible))
        }
    }

    fun clearSection() {
        _uiState.update { state ->
            state.points.forEach { point -> point.anchor.detach() }
            FloorUiState(
                trackingState = state.trackingState,
                horizontalPlaneCount = state.horizontalPlaneCount,
                selectedArea = state.selectedArea,
                hasCenterHit = state.hasCenterHit,
                isDepthEnabled = state.isDepthEnabled,
                status = state.status,
                instruction = state.instruction,
                detectionState = state.detectionState
            )
        }
    }

    fun rotateTexture() {
        _uiState.update { state ->
            if (!state.isTileVisible) return@update state
            val nextOrdinal = (state.textureRotation.ordinal + 1) % TextureRotation.entries.size
            state.copy(
                textureRotation = TextureRotation.entries[nextOrdinal]
            )
        }
    }

    fun changeTileType() {
        _uiState.update { state ->
            if (!state.isContourConfirmed || !state.isTileVisible) return@update state
            val nextOrdinal = (state.selectedTileType.ordinal + 1) % TileType.entries.size
            state.copy(
                selectedTileType = TileType.entries[nextOrdinal]
            )
        }
    }
    
    fun reset() {
        _uiState.update { state ->
            state.points.forEach { point -> point.anchor.detach() }
            FloorUiState()
        }
    }

    override fun onCleared() {
        _uiState.value.points.forEach { point -> point.anchor.detach() }
        super.onCleared()
    }
    
    private fun calculateDistance(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float {
        return sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2) + (z1 - z2) * (z1 - z2))
    }
    
    companion object {
        private const val CLOSE_THRESHOLD_M = 0.10f
        private const val SNAP_THRESHOLD_M = 0.05f
        private const val MAX_POINT_HEIGHT_DELTA_M = 0.08f
    }
}
