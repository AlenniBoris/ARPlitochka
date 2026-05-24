package com.example.arplitka.features.floordetection.presentation.viewmodel

import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import com.example.arplitka.features.floordetection.R
import com.example.arplitka.features.floordetection.domain.model.ArPoint
import com.example.arplitka.features.floordetection.domain.model.FloorDetectionState
import com.example.arplitka.features.floordetection.domain.model.FloorUiState
import com.example.arplitka.features.floordetection.domain.usecase.ProcessArFrameUseCase
import com.example.arplitka.shared.ui.UiText
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
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
            var newState = if (result.trackingState != TrackingState.TRACKING) {
                currentState.copy(
                    trackingState = result.trackingState,
                    horizontalPlaneCount = result.horizontalPlaneCount,
                    hasCenterHit = false,
                    isDepthEnabled = result.isDepthEnabled,
                    statusText = UiText.StringResource(R.string.status_tracking_lost),
                    instructionText = UiText.StringResource(R.string.instruction_move_phone),
                    currentHitPose = null,
                    snappedPointIndex = null
                )
            } else if (!result.isFloorDetected) {
                currentState.copy(
                    detectionState = FloorDetectionState.SearchingFloor,
                    trackingState = result.trackingState,
                    horizontalPlaneCount = result.horizontalPlaneCount,
                    selectedArea = result.selectedArea,
                    hasCenterHit = result.hasCenterHit,
                    isDepthEnabled = result.isDepthEnabled,
                    statusText = UiText.StringResource(R.string.status_searching),
                    instructionText = UiText.StringResource(R.string.instruction_searching),
                    currentHitPose = result.hitPose,
                    snappedPointIndex = null
                )
            } else {
                currentState.copy(
                    detectionState = FloorDetectionState.CandidateFound,
                    trackingState = result.trackingState,
                    horizontalPlaneCount = result.horizontalPlaneCount,
                    selectedArea = result.selectedArea,
                    hasCenterHit = true,
                    isDepthEnabled = result.isDepthEnabled,
                    statusText = if (currentState.isPolygonClosed) 
                        UiText.StringResource(R.string.polygon_closed) 
                        else UiText.StringResource(R.string.status_candidate),
                    instructionText = UiText.StringResource(R.string.instruction_detected),
                    currentHitPose = result.hitPose
                )
            }
            
            // Snapping and Polygon Closing logic
            if (!newState.isFinalized && newState.hasCenterHit && newState.currentHitPose != null && newState.points.isNotEmpty()) {
                val currentPose = newState.currentHitPose!!
                
                // Check snapping to the FIRST point (for closing)
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
                    // Check snapping to ANY other point (optional, but requested "if point and reticle are at insignificant distance")
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
            } else {
                newState = newState.copy(snappedPointIndex = null)
            }
            
            newState
        }
    }
    
    fun addPoint() {
        _uiState.update { state ->
            if (state.isFinalized) return@update state
            
            if (state.isPolygonClosed) {
                return@update state.copy(isFinalized = true)
            }

            val snappedIndex = state.snappedPointIndex
            if (snappedIndex != null && snappedIndex == state.points.lastIndex) {
                return@update state
            }

            val poseToAdd = if (snappedIndex != null) {
                state.points[snappedIndex].pose
            } else {
                state.currentHitPose
            } ?: return@update state

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
            
            state.copy(points = state.points + ArPoint(poseToAdd))
        }
    }
    
    fun undoPoint() {
        _uiState.update { state ->
            if (state.isFinalized) return@update state
            if (state.points.isNotEmpty()) {
                state.copy(
                    points = state.points.dropLast(1),
                    isPolygonClosed = false
                )
            } else {
                state
            }
        }
    }

    fun clearSection() {
        _uiState.update { state ->
            state.copy(
                points = emptyList(),
                isPolygonClosed = false,
                isFinalized = false,
                snappedPointIndex = null
            )
        }
    }
    
    fun reset() {
        _uiState.update { FloorUiState() }
    }
    
    private fun calculateDistance(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float {
        return sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2) + (z1 - z2) * (z1 - z2))
    }
    
    companion object {
        private const val CLOSE_THRESHOLD_M = 0.10f // 10cm to close the loop
        private const val SNAP_THRESHOLD_M = 0.05f  // 5cm for visual snapping
        private const val REOPEN_THRESHOLD_M = 0.15f
    }
}
