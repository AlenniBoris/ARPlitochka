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
                trackingStatus = if (state.isFinalized && state.isPolygonClosed) {
                    ArTrackingStatus.FINALIZED
                } else if (state.isPolygonClosed) {
                    ArTrackingStatus.POLYGON_CLOSED
                } else {
                    ArTrackingStatus.TRACKING_LOST
                },
                instruction = when {
                    state.isFinalized -> ArInstruction.EMPTY
                    state.isPolygonClosed -> ArInstruction.CONTOUR_CLOSED
                    else -> ArInstruction.MOVE_PHONE
                },
                hasCenterHit = false,
                currentHitPoint = null,
                snappedPointIndex = null,
                isPolygonClosed = if (state.isFinalized) state.isPolygonClosed else false,
                horizontalPlaneCount = snapshot.horizontalPlaneCount,
                selectedArea = snapshot.selectedArea,
                largestPlaneAreaM2 = snapshot.largestPlaneAreaM2,
                isFloorDetected = false,
                focusedLabel = snapshot.focusedLabel
            )
            !snapshot.isFloorDetected -> {
                val contouring = state.placedPoints.size >= 2 && snapshot.currentHitPoint != null
                state.copy(
                    trackingStatus = when {
                        state.isFinalized && state.isPolygonClosed -> ArTrackingStatus.FINALIZED
                        state.isPolygonClosed -> ArTrackingStatus.POLYGON_CLOSED
                        contouring -> ArTrackingStatus.FLOOR_DETECTED
                        else -> ArTrackingStatus.SEARCHING_FLOOR
                    },
                    instruction = when {
                        state.isFinalized -> ArInstruction.EMPTY
                        state.isPolygonClosed -> ArInstruction.CONTOUR_CLOSED
                        contouring -> ArInstruction.DETECTED
                        snapshot.largestPlaneAreaM2 >= MIN_SCAN_SURFACE_AREA_M2 -> ArInstruction.SURFACE_NEARBY
                        else -> ArInstruction.SEARCHING
                    },
                    hasCenterHit = snapshot.hasCenterHit || contouring,
                    currentHitPoint = snapshot.currentHitPoint,
                    snappedPointIndex = if (contouring && !state.isFinalized) state.snappedPointIndex else null,
                    isPolygonClosed = when {
                        state.isFinalized -> state.isPolygonClosed
                        contouring -> state.isPolygonClosed
                        else -> false
                    },
                    horizontalPlaneCount = snapshot.horizontalPlaneCount,
                    selectedArea = snapshot.selectedArea,
                    largestPlaneAreaM2 = snapshot.largestPlaneAreaM2,
                    isFloorDetected = contouring,
                    focusedLabel = snapshot.focusedLabel
                )
            }
            else -> state.copy(
                trackingStatus = when {
                    state.isFinalized && state.isPolygonClosed -> ArTrackingStatus.FINALIZED
                    state.isPolygonClosed -> ArTrackingStatus.POLYGON_CLOSED
                    else -> ArTrackingStatus.FLOOR_DETECTED
                },
                instruction = when {
                    state.isFinalized -> ArInstruction.EMPTY
                    state.isPolygonClosed -> ArInstruction.CONTOUR_CLOSED
                    else -> ArInstruction.DETECTED
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
        val snapped = FloorSnapReducer.applySnap(trackingFields)
        val preserveSnapDuringPlacement = state.placedPoints.size >= 2 &&
            !state.isFinalized &&
            snapshot.currentHitPoint == null &&
            (state.isPolygonClosed || state.snappedPointIndex != null)
        val preserveFinalizedClosed = state.isFinalized && state.isPolygonClosed
        return when {
            preserveFinalizedClosed -> snapped.copy(
                isPolygonClosed = true,
                snappedPointIndex = null,
                trackingStatus = ArTrackingStatus.FINALIZED,
                instruction = ArInstruction.EMPTY
            )
            preserveSnapDuringPlacement -> snapped.copy(
                snappedPointIndex = state.snappedPointIndex,
                isPolygonClosed = state.isPolygonClosed
            )
            else -> snapped
        }
    }
}
