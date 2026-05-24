package com.example.arplitka.features.floordetection.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import com.example.arplitka.features.floordetection.R
import com.example.arplitka.features.floordetection.domain.model.FloorDetectionState
import com.example.arplitka.features.floordetection.domain.model.FloorUiState
import com.example.arplitka.features.floordetection.domain.usecase.ProcessArFrameUseCase
import com.example.arplitka.shared.ui.UiText
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FloorArViewModel @Inject constructor(
    private val processArFrameUseCase: ProcessArFrameUseCase
) : ViewModel() {
    
    var uiState by mutableStateOf(FloorUiState())
        private set
    
    fun onSessionUpdated(session: Session, frame: Frame, viewportSize: IntSize) {
        val result = processArFrameUseCase(session, frame, viewportSize)
        
        uiState = if (result.trackingState != TrackingState.TRACKING) {
            uiState.copy(
                trackingState = result.trackingState,
                horizontalPlaneCount = result.horizontalPlaneCount,
                hasCenterHit = false,
                isDepthEnabled = result.isDepthEnabled,
                statusText = UiText.StringResource(R.string.status_tracking_lost),
                instructionText = UiText.StringResource(R.string.instruction_move_phone),
            )
        } else if (!result.isFloorDetected) {
            FloorUiState(
                detectionState = FloorDetectionState.SearchingFloor,
                trackingState = result.trackingState,
                horizontalPlaneCount = result.horizontalPlaneCount,
                selectedArea = result.selectedArea,
                hasCenterHit = result.hasCenterHit,
                isDepthEnabled = result.isDepthEnabled,
                statusText = UiText.StringResource(R.string.status_searching),
                instructionText = UiText.StringResource(R.string.instruction_searching)
            )
        } else {
            FloorUiState(
                detectionState = FloorDetectionState.CandidateFound,
                trackingState = result.trackingState,
                horizontalPlaneCount = result.horizontalPlaneCount,
                selectedArea = result.selectedArea,
                hasCenterHit = true,
                isDepthEnabled = result.isDepthEnabled,
                statusText = UiText.StringResource(R.string.status_candidate),
                instructionText = UiText.StringResource(R.string.instruction_detected)
            )
        }
    }
    
    fun reset() {
        uiState = FloorUiState()
    }
}
