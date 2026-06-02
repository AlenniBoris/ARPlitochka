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
        val distToFirst = FloorGeometry.distance(first, current)

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
            if (FloorGeometry.distance(point, current) < FloorGeometry.SNAP_THRESHOLD_M) {
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

object FloorContourReducer {
    fun tryAddPoint(state: FloorContourUiState): ArPoint3D? {
        if (state.isFinalized) return null
        if (state.isPolygonClosed || state.snappedPointIndex != null) return null
        if (!state.hasCenterHit || state.currentHitPoint == null) return null

        val point = state.currentHitPoint
        val last = state.placedPoints.lastOrNull()?.position
        if (last != null && FloorGeometry.distance(last, point) < FloorGeometry.SNAP_THRESHOLD_M) {
            return null
        }

        val floorY = state.placedPoints.firstOrNull()?.position?.yMeters
        if (floorY != null && !FloorGeometry.isWithinHeightTolerance(point, floorY)) {
            return null
        }

        return point
    }
}
