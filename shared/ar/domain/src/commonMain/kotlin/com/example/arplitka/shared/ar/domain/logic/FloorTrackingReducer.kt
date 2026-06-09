package com.example.arplitka.shared.ar.domain.logic

import com.example.arplitka.shared.ar.contracts.model.ArInstruction
import com.example.arplitka.shared.ar.contracts.model.ArTrackingStatus
import com.example.arplitka.shared.ar.domain.model.FloorContourUiState
import com.example.arplitka.shared.ar.domain.model.FloorFrameSnapshot

object FloorTrackingReducer {
    private const val MIN_SCAN_SURFACE_AREA_M2 = 0.15f

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
                largestPlaneAreaM2 = snapshot.largestPlaneAreaM2,
                isFloorDetected = false,
                focusedLabel = snapshot.focusedLabel
            )
            !snapshot.isFloorDetected -> {
                val contouring = state.placedPoints.size >= 2 && snapshot.currentHitPoint != null
                state.copy(
                    trackingStatus = if (state.isPolygonClosed) {
                        ArTrackingStatus.POLYGON_CLOSED
                    } else if (contouring) {
                        ArTrackingStatus.FLOOR_DETECTED
                    } else {
                        ArTrackingStatus.SEARCHING_FLOOR
                    },
                    instruction = when {
                        state.isPolygonClosed -> ArInstruction.CONTOUR_CLOSED
                        contouring -> ArInstruction.DETECTED
                        snapshot.largestPlaneAreaM2 >= MIN_SCAN_SURFACE_AREA_M2 -> ArInstruction.SURFACE_NEARBY
                        else -> ArInstruction.SEARCHING
                    },
                    hasCenterHit = snapshot.hasCenterHit || contouring,
                    currentHitPoint = snapshot.currentHitPoint,
                    snappedPointIndex = null,
                    horizontalPlaneCount = snapshot.horizontalPlaneCount,
                    selectedArea = snapshot.selectedArea,
                    largestPlaneAreaM2 = snapshot.largestPlaneAreaM2,
                    isFloorDetected = contouring,
                    focusedLabel = snapshot.focusedLabel
                )
            }
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
                largestPlaneAreaM2 = snapshot.largestPlaneAreaM2,
                isFloorDetected = true,
                focusedLabel = snapshot.focusedLabel
            )
        }
        return FloorSnapReducer.applySnap(trackingFields)
    }
}
