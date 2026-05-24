package com.example.arplitka.features.floordetection.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import com.example.arplitka.features.floordetection.domain.model.FloorDetectionState
import com.example.arplitka.features.floordetection.domain.model.FloorUiState
import com.example.arplitka.features.floordetection.domain.usecase.ProcessArFrameUseCase
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
                statusText = "Трекинг потерян",
                instructionText = "Медленно наведите камеру на пол",
            )
        } else if (!result.isFloorDetected) {
            FloorUiState(
                detectionState = FloorDetectionState.SearchingFloor,
                trackingState = result.trackingState,
                horizontalPlaneCount = result.horizontalPlaneCount,
                selectedArea = result.selectedArea,
                hasCenterHit = result.hasCenterHit,
                isDepthEnabled = result.isDepthEnabled,
                statusText = "Наведите камеру на пол",
                instructionText = "Ищем подходящую поверхность"
            )
        } else {
            FloorUiState(
                detectionState = FloorDetectionState.CandidateFound,
                trackingState = result.trackingState,
                horizontalPlaneCount = result.horizontalPlaneCount,
                selectedArea = result.selectedArea,
                hasCenterHit = true,
                isDepthEnabled = result.isDepthEnabled,
                statusText = "Пол обнаружен",
                instructionText = "Поверхность отображается точками"
            )
        }
    }
    
    fun reset() {
        uiState = FloorUiState()
    }
}
