package com.example.arplitka.shared.ar.domain.logic

import com.example.arplitka.shared.ar.contracts.model.ArPoint3D
import com.example.arplitka.shared.ar.domain.model.FloorContourUiState
import com.example.arplitka.shared.ar.domain.model.FloorFrameSnapshot
import com.example.arplitka.shared.ar.domain.model.PlacedContourPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FloorContourReducerTest {
    private fun point(x: Float, y: Float = 0f, z: Float = 0f) = ArPoint3D(x, y, z)

    private fun stateWithPoints(vararg coords: Pair<Float, Float>): FloorContourUiState {
        val placed = coords.mapIndexed { index, (x, z) ->
            PlacedContourPoint(id = "p$index", position = point(x, z = z))
        }
        return FloorContourUiState(
            hasCenterHit = true,
            isFloorDetected = true,
            placedPoints = placed,
            currentHitPoint = point(0f, 0f, 0f)
        )
    }

    @Test
    fun tryAddPoint_rejectsWhenTooCloseToLastPoint() {
        val state = stateWithPoints(0f to 0f).copy(
            currentHitPoint = point(0.01f, 0f, 0.01f)
        )
        assertNull(FloorContourReducer.tryAddPoint(state))
    }

    @Test
    fun validateAddPoint_projectsToSectionFloor() {
        val state = stateWithPoints(0f to 0f).copy(
            currentHitPoint = point(1f, 0.02f, 0f)
        )
        val result = FloorContourReducer.validateAddPoint(state, point(1f, 0.02f, 0f))
        assertEquals(point(1f, 0f, 0f), assertIs<AddPointValidation.Accepted>(result).point)
    }

    @Test
    fun tryAddPoint_rejectsWhenHeightDeltaTooLarge() {
        val state = stateWithPoints(0f to 0f, 1f to 0f).copy(
            currentHitPoint = point(2f, 0.2f, 0f)
        )
        assertNull(FloorContourReducer.tryAddPoint(state))
    }

    @Test
    fun validateTapPlacement_acceptsWithoutLiveCenterHit() {
        val state = stateWithPoints(0f to 0f).copy(
            hasCenterHit = false,
            currentHitPoint = null
        )
        val result = FloorContourReducer.validateTapPlacement(state, point(1f, 0f, 0f))
        assertEquals(point(1f, 0f, 0f), assertIs<AddPointValidation.Accepted>(result).point)
    }

    @Test
    fun tryAddPoint_acceptsValidPoint() {
        val state = stateWithPoints(0f to 0f).copy(
            currentHitPoint = point(1f, 0f, 0f)
        )
        assertEquals(point(1f, 0f, 0f), FloorContourReducer.tryAddPoint(state))
    }

    @Test
    fun snapReducer_closesPolygonNearFirstPoint() {
        val state = stateWithPoints(
            0f to 0f,
            1f to 0f,
            1f to 1f
        ).copy(currentHitPoint = point(0.05f, 0f, 0.05f))

        val snapped = FloorSnapReducer.applySnap(state)
        assertEquals(true, snapped.isPolygonClosed)
        assertEquals(0, snapped.snappedPointIndex)
    }

    @Test
    fun snapReducer_closesWithoutLiveCenterHitWhenContourExists() {
        val state = stateWithPoints(
            0f to 0f,
            1f to 0f,
            1f to 1f
        ).copy(
            hasCenterHit = false,
            currentHitPoint = point(0.05f, 0f, 0.05f)
        )

        val snapped = FloorSnapReducer.applySnap(state)
        assertEquals(true, snapped.isPolygonClosed)
        assertEquals(0, snapped.snappedPointIndex)
    }

    @Test
    fun snapReducer_preservesClosedWhenFinalized() {
        val state = stateWithPoints(
            0f to 0f,
            1f to 0f,
            1f to 1f
        ).copy(
            isFinalized = true,
            isPolygonClosed = true,
            currentHitPoint = null
        )

        val snapped = FloorSnapReducer.applySnap(state)
        assertTrue(snapped.isPolygonClosed)
        assertNull(snapped.snappedPointIndex)
    }

    @Test
    fun snapReducer_snapsToExistingPoint() {
        val state = stateWithPoints(
            0f to 0f,
            1f to 0f
        ).copy(currentHitPoint = point(1f, 0f, 0.015f))

        val snapped = FloorSnapReducer.applySnap(state)
        assertEquals(false, snapped.isPolygonClosed)
        assertEquals(1, snapped.snappedPointIndex)
    }

    @Test
    fun closedPlacementState_showsPointsLinesAndFill() {
        val state = stateWithPoints(
            0f to 0f,
            1f to 0f,
            1f to 1f
        ).copy(
            isPolygonClosed = true,
            snappedPointIndex = 0
        )

        assertTrue(state.showContourPoints)
        assertTrue(state.showContourLines)
        assertTrue(state.showSectionFill)
    }

    @Test
    fun finalizedState_keepsPointsLinesAndFill() {
        val state = stateWithPoints(
            0f to 0f,
            1f to 0f,
            1f to 1f
        ).copy(
            isFinalized = true,
            isPolygonClosed = true
        )

        assertTrue(state.showContourPoints)
        assertTrue(state.showContourLines)
        assertTrue(state.showSectionFill)
    }

    @Test
    fun finalizedClosedState_persistsAfterApplyFrame() {
        val state = stateWithPoints(
            0f to 0f,
            1f to 0f,
            1f to 1f
        ).copy(
            isFinalized = true,
            isPolygonClosed = true
        )
        val snapshot = FloorFrameSnapshot(
            isTracking = true,
            horizontalPlaneCount = 1,
            selectedArea = 1f,
            hasCenterHit = false,
            isFloorDetected = true,
            currentHitPoint = null,
            focusedLabel = "contour",
            largestPlaneAreaM2 = 1f
        )

        val updated = FloorTrackingReducer.applyFrame(state, snapshot)
        assertTrue(updated.isPolygonClosed)
        assertTrue(updated.isFinalized)
        assertTrue(updated.showSectionFill)
        assertTrue(updated.showContourLines)
        assertTrue(updated.showContourPoints)
    }

    @Test
    fun trackingReducer_preservesClosedSnapWhenHitMissingDuringPlacement() {
        val state = stateWithPoints(
            0f to 0f,
            1f to 0f,
            1f to 1f
        ).copy(
            isPolygonClosed = true,
            snappedPointIndex = 0
        )
        val snapshot = FloorFrameSnapshot(
            isTracking = true,
            horizontalPlaneCount = 1,
            selectedArea = 1f,
            hasCenterHit = false,
            isFloorDetected = false,
            currentHitPoint = null,
            focusedLabel = "placement",
            largestPlaneAreaM2 = 1f
        )

        val updated = FloorTrackingReducer.applyFrame(state, snapshot)
        assertTrue(updated.isPolygonClosed)
        assertEquals(0, updated.snappedPointIndex)
    }
}
