package com.example.arplitka.shared.ar.domain.model

import com.example.arplitka.shared.ar.contracts.model.ArInstruction
import com.example.arplitka.shared.ar.contracts.model.ArPoint3D
import com.example.arplitka.shared.ar.contracts.model.ArTrackingStatus

data class FloorContourUiState(
    val trackingStatus: ArTrackingStatus = ArTrackingStatus.INITIALIZING,
    val instruction: ArInstruction = ArInstruction.PLEASE_WAIT,
    val horizontalPlaneCount: Int = 0,
    val selectedArea: Float = 0f,
    val hasCenterHit: Boolean = false,
    val isFloorDetected: Boolean = false,
    val focusedLabel: String = "",
    val placedPoints: List<PlacedContourPoint> = emptyList(),
    val isPolygonClosed: Boolean = false,
    val isFinalized: Boolean = false,
    val isTileVisible: Boolean = false,
    val snappedPointIndex: Int? = null,
    val currentHitPoint: ArPoint3D? = null
) {
    val showContourPoints: Boolean
        get() = placedPoints.isNotEmpty() && !isFinalized

    val showContourLines: Boolean
        get() = placedPoints.size >= 2 && !isTileVisible && !isFinalized

    val showPreviewLine: Boolean
        get() = !isFinalized &&
                !isPolygonClosed &&
                placedPoints.isNotEmpty() &&
                currentHitPoint != null

    val showPlaneDots: Boolean
        get() = !isFinalized

    val showPlaneRenderer: Boolean
        get() = showPlaneDots

    val showContourActions: Boolean
        get() = !isFinalized

    val showSectionFill: Boolean
        get() = isFinalized && isPolygonClosed && placedPoints.size >= 3

    val showTileControls: Boolean
        get() = isFinalized && isPolygonClosed && placedPoints.size >= 3
}
