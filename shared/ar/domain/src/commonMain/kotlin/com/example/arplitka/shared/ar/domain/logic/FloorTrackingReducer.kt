package com.example.arplitka.shared.ar.domain.logic

import com.example.arplitka.shared.ar.contracts.model.ArInstruction
import com.example.arplitka.shared.ar.contracts.model.ArTrackingStatus
import com.example.arplitka.shared.ar.domain.model.FloorContourUiState
import com.example.arplitka.shared.ar.domain.model.FloorFrameSnapshot

object FloorTrackingReducer {
    fun applyFrame(
        state: FloorContourUiState,
        snapshot: FloorFrameSnapshot
    ): FloorContourUiState {
        val trackingFields = when {
            !snapshot.isTracking -> state.copy(
                trackingStatus = ArTrackingStatus.TRACKING_LOST,
                instruction = ArInstruction.MOVE_PHONE,
                hasCenterHit = false,
                currentHitPoint = null,
                snappedPointIndex = null,
                horizontalPlaneCount = snapshot.horizontalPlaneCount,
                selectedArea = snapshot.selectedArea,
                isFloorDetected = false,
                focusedLabel = snapshot.focusedLabel
            )
            !snapshot.isFloorDetected -> state.copy(
                trackingStatus = ArTrackingStatus.SEARCHING_FLOOR,
                instruction = ArInstruction.SEARCHING,
                hasCenterHit = snapshot.hasCenterHit,
                currentHitPoint = snapshot.currentHitPoint,
                snappedPointIndex = null,
                horizontalPlaneCount = snapshot.horizontalPlaneCount,
                selectedArea = snapshot.selectedArea,
                isFloorDetected = false,
                focusedLabel = snapshot.focusedLabel
            )
            else -> state.copy(
                trackingStatus = if (state.isPolygonClosed) {
                    ArTrackingStatus.POLYGON_CLOSED
                } else {
                    ArTrackingStatus.FLOOR_DETECTED
                },
                instruction = if (state.isPolygonClosed) {
                    ArInstruction.CONTOUR_CLOSED
                } else {
                    ArInstruction.DETECTED
                },
                hasCenterHit = true,
                currentHitPoint = snapshot.currentHitPoint,
                horizontalPlaneCount = snapshot.horizontalPlaneCount,
                selectedArea = snapshot.selectedArea,
                isFloorDetected = true,
                focusedLabel = snapshot.focusedLabel
            )
        }
        return FloorSnapReducer.applySnap(trackingFields)
    }
}
