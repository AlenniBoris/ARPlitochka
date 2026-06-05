package com.example.arplitka.shared.ar.domain.logic

import com.example.arplitka.shared.ar.contracts.model.ArPoint3D
import com.example.arplitka.shared.ar.domain.model.FloorContourUiState

object FloorSnapReducer {
    fun applySnap(state: FloorContourUiState): FloorContourUiState {
        if (
            state.isFinalized ||
            !state.hasCenterHit ||
            state.currentHitPoint == null ||
            state.placedPoints.isEmpty()
        ) {
            return state.copy(snappedPointIndex = null, isPolygonClosed = false)
        }

        val current = state.currentHitPoint
        val first = state.placedPoints.first().position
        val distToFirst = FloorGeometry.distancePlanar(first, current)

        if (state.placedPoints.size >= 3 && distToFirst < FloorGeometry.CLOSE_THRESHOLD_M) {
            return state.copy(
                isPolygonClosed = true,
                snappedPointIndex = 0,
                trackingStatus = com.example.arplitka.shared.ar.contracts.model.ArTrackingStatus.POLYGON_CLOSED
            )
        }

        if (distToFirst < FloorGeometry.SNAP_THRESHOLD_M) {
            return state.copy(
                snappedPointIndex = 0,
                isPolygonClosed = false
            )
        }

        for (index in 1 until state.placedPoints.size) {
            val point = state.placedPoints[index].position
            if (FloorGeometry.distancePlanar(point, current) < FloorGeometry.SNAP_THRESHOLD_M) {
                return state.copy(
                    snappedPointIndex = index,
                    isPolygonClosed = false
                )
            }
        }

        return state.copy(
            snappedPointIndex = null,
            isPolygonClosed = false
        )
    }
}

enum class AddPointRejectReason {
    FINALIZED,
    POLYGON_CLOSED,
    SNAP_ACTIVE,
    NO_HIT,
    TOO_CLOSE_TO_LAST,
    HEIGHT_OUT_OF_RANGE
}

object FloorContourReducer {
    fun tryAddPoint(state: FloorContourUiState): ArPoint3D? {
        val candidate = state.currentHitPoint ?: return null
        return when (val validation = validateAddPoint(state, candidate)) {
            is AddPointValidation.Accepted -> validation.point
            is AddPointValidation.Rejected -> null
        }
    }

    fun validateAddPoint(
        state: FloorContourUiState,
        candidate: ArPoint3D
    ): AddPointValidation {
        rejectReason(state, candidate)?.let { return AddPointValidation.Rejected(it) }
        val sectionFloorY = state.placedPoints.firstOrNull()?.position?.yMeters
        return AddPointValidation.Accepted(FloorGeometry.projectToSectionFloor(candidate, sectionFloorY))
    }

    private fun rejectReason(state: FloorContourUiState, candidate: ArPoint3D): AddPointRejectReason? {
        when {
            state.isFinalized -> return AddPointRejectReason.FINALIZED
            state.isPolygonClosed -> return AddPointRejectReason.POLYGON_CLOSED
            state.snappedPointIndex != null -> return AddPointRejectReason.SNAP_ACTIVE
            !state.hasCenterHit -> return AddPointRejectReason.NO_HIT
        }
        val last = state.placedPoints.lastOrNull()?.position
        if (last != null && FloorGeometry.distancePlanar(last, candidate) < FloorGeometry.SNAP_THRESHOLD_M) {
            return AddPointRejectReason.TOO_CLOSE_TO_LAST
        }
        val floorY = state.placedPoints.firstOrNull()?.position?.yMeters
        if (floorY != null && !FloorGeometry.isWithinHeightTolerance(candidate, floorY)) {
            return AddPointRejectReason.HEIGHT_OUT_OF_RANGE
        }
        return null
    }
}

sealed class AddPointValidation {
    data class Accepted(val point: ArPoint3D) : AddPointValidation()
    data class Rejected(val reason: AddPointRejectReason) : AddPointValidation()
}
