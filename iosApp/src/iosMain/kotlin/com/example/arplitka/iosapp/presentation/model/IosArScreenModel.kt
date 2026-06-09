package com.example.arplitka.iosapp.presentation.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.arplitka.iosapp.presentation.debug.IosPlaneDebugMetrics
import com.example.arplitka.iosapp.platform.ar.IosArSessionCoordinator
import com.example.arplitka.shared.ar.domain.FloorArController
import com.example.arplitka.shared.ar.domain.model.FloorContourUiState

/**
 * iOS presentation state holder for the AR screen.
 *
 * Owns UI-facing state and wires the ARKit coordinator to the shared domain
 * controller without rendering UI.
 */
internal class IosArScreenModel {
    var contourState by mutableStateOf(FloorContourUiState())
        private set

    var trackingStateName by mutableStateOf("INITIALIZING")
        private set

    var planeDebugMetrics by mutableStateOf(IosPlaneDebugMetrics())
        private set

    var placementHint by mutableStateOf<String?>(null)
        private set

    private val floorArController = FloorArController(
        onStateChanged = { contourState = it }
    )

    val coordinator = IosArSessionCoordinator(
        floorArController = floorArController,
        onTrackingNameChanged = { trackingStateName = it },
        onPlaneDebugMetricsChanged = { planeDebugMetrics = it },
        onPlacementHintChanged = { placementHint = it }
    )

    fun pause() {
        coordinator.pause()
    }
}
