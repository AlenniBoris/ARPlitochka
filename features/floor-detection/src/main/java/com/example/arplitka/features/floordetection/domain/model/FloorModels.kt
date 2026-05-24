package com.example.arplitka.features.floordetection.domain.model

import com.example.arplitka.features.floordetection.R
import com.example.arplitka.shared.ui.UiText
import com.google.ar.core.TrackingState

enum class FloorDetectionState {
    SearchingFloor,
    CandidateFound
}

data class FloorUiState(
    val detectionState: FloorDetectionState = FloorDetectionState.SearchingFloor,
    val trackingState: TrackingState = TrackingState.STOPPED,
    val horizontalPlaneCount: Int = 0,
    val selectedArea: Float = 0f,
    val hasCenterHit: Boolean = false,
    val isDepthEnabled: Boolean = false,
    val statusText: UiText = UiText.StringResource(R.string.initialization),
    val instructionText: UiText = UiText.StringResource(R.string.please_wait)
)

/**
 * Domain model representing the result of processing an AR frame.
 * This is what the Repository returns to the Domain/Presentation layer.
 */
data class ArFrameResult(
    val trackingState: TrackingState,
    val horizontalPlaneCount: Int,
    val selectedArea: Float,
    val hasCenterHit: Boolean,
    val isFloorDetected: Boolean,
    val isDepthEnabled: Boolean
)
