package com.example.arplitka.shared.ar.domain.model

import com.example.arplitka.shared.ar.contracts.model.ArInstruction
import com.example.arplitka.shared.ar.contracts.model.ArPoint3D
import com.example.arplitka.shared.ar.contracts.model.ArTrackingStatus

data class FloorContourUiState(
    val stage: FloorWorkflowStage = FloorWorkflowStage.INITIALIZING,
    val trackingStatus: ArTrackingStatus = ArTrackingStatus.INITIALIZING,
    val instruction: ArInstruction = ArInstruction.PLEASE_WAIT,
    val horizontalPlaneCount: Int = 0,
    val selectedArea: Float = 0f,
    val largestPlaneAreaM2: Float = 0f,
    val hasCenterHit: Boolean = false,
    val isFloorDetected: Boolean = false,
    val focusedLabel: String = "",
    val placedPoints: List<PlacedContourPoint> = emptyList(),
    val isPolygonClosed: Boolean = false,
    val isFinalized: Boolean = false,
    val isTileVisible: Boolean = false,
    val selectedTileType: TileType = TileType.PAVING_STONES_V2,
    val textureRotation: TextureRotation = TextureRotation.DEGREES_0,
    val snappedPointIndex: Int? = null,
    val currentHitPoint: ArPoint3D? = null
) {
    val showContourPoints: Boolean
        get() = placedPoints.isNotEmpty() && stage != FloorWorkflowStage.TILE_LAYOUT

    val showContourLines: Boolean
        get() = placedPoints.size >= 2 && stage != FloorWorkflowStage.TILE_LAYOUT

    val showPreviewLine: Boolean
        get() = stage == FloorWorkflowStage.PLACEMENT_ACTIVE || stage == FloorWorkflowStage.PLACEMENT_EMPTY

    val showPlaneDots: Boolean
        get() = stage.ordinal < FloorWorkflowStage.CONTOUR_CONFIRMED.ordinal

    val showPlaneRenderer: Boolean
        get() = showPlaneDots

    val showContourActions: Boolean
        get() = stage.ordinal < FloorWorkflowStage.CONTOUR_CONFIRMED.ordinal

    val showSectionFill: Boolean
        get() = (stage == FloorWorkflowStage.CONTOUR_CLOSED || stage.ordinal >= FloorWorkflowStage.CONTOUR_CONFIRMED.ordinal) && placedPoints.size >= 3

    val showTileControls: Boolean
        get() = stage == FloorWorkflowStage.TILE_LAYOUT
}
