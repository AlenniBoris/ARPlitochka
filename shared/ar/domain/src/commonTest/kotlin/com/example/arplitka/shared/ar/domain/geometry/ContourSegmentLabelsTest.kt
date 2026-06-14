package com.example.arplitka.shared.ar.domain.geometry

import com.example.arplitka.shared.ar.contracts.model.ArPoint3D
import com.example.arplitka.shared.ar.domain.model.FloorContourUiState
import com.example.arplitka.shared.ar.domain.model.PlacedContourPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ContourSegmentLabelsTest {
    private fun point(x: Float, z: Float, y: Float = 0f) = ArPoint3D(x, y, z)

    private fun stateWithPoints(
        vararg coords: Pair<Float, Float>,
        isPolygonClosed: Boolean = false,
        isFinalized: Boolean = false
    ): FloorContourUiState {
        val placed = coords.mapIndexed { index, (x, z) ->
            PlacedContourPoint(id = "p$index", position = point(x, z))
        }
        return FloorContourUiState(
            hasCenterHit = true,
            isFloorDetected = true,
            placedPoints = placed,
            isPolygonClosed = isPolygonClosed,
            isFinalized = isFinalized
        )
    }

    @Test
    fun formatContourDistanceMeters_roundsToCentimeters() {
        assertEquals("1.23 м", 1.234f.formatContourDistanceMeters())
        assertEquals("0.50 м", 0.5f.formatContourDistanceMeters())
    }

    @Test
    fun buildContourSegmentLabels_returnsEmptyWhenFewerThanTwoPoints() {
        val state = stateWithPoints(0f to 0f)
        assertTrue(buildContourSegmentLabels(state, floorY = 0f).isEmpty())
    }

    @Test
    fun buildContourSegmentLabels_returnsAdjacentSegmentsForOpenContour() {
        val state = stateWithPoints(0f to 0f, 1f to 0f, 1f to 1f)
        val segments = buildContourSegmentLabels(state, floorY = 0f)
        assertEquals(2, segments.size)
        assertEquals("p0-p1", segments[0].key)
        assertEquals("p1-p2", segments[1].key)
        assertEquals(1f, segments[0].distanceMeters)
    }

    @Test
    fun buildContourSegmentLabels_addsClosingSegmentWhenPolygonClosed() {
        val state = stateWithPoints(
            0f to 0f,
            1f to 0f,
            1f to 1f,
            isPolygonClosed = true
        )
        val segments = buildContourSegmentLabels(state, floorY = 0f)
        assertEquals(3, segments.size)
        assertEquals("p2-p0-close", segments.last().key)
        assertEquals(1.4142135f, segments.last().distanceMeters, 0.001f)
    }

    @Test
    fun buildContourSegmentLabels_usesPlanarDistanceOnFloor() {
        val state = stateWithPoints(0f to 0f, 3f to 4f)
        val segment = buildContourSegmentLabels(state, floorY = 0f).single()
        assertEquals(5f, segment.distanceMeters)
    }

    @Test
    fun contourDistanceLabelBatchKey_changesWhenMidpointMoves() {
        val stateA = stateWithPoints(0f to 0f, 1f to 0f)
        val stateB = stateWithPoints(0f to 0f, 2f to 0f)
        val keyA = contourDistanceLabelBatchKey(buildContourSegmentLabels(stateA, 0f), 0f, true)
        val keyB = contourDistanceLabelBatchKey(buildContourSegmentLabels(stateB, 0f), 0f, true)
        assertNotEquals(keyA, keyB)
    }

    @Test
    fun contourLineRotationYDegrees_alignsWithSegmentDirection() {
        assertEquals(0f, contourLineRotationYDegrees(1f, 0f), 0.001f)
        assertEquals(-90f, contourLineRotationYDegrees(0f, 1f), 0.001f)
        assertEquals(90f, contourLineRotationYDegrees(0f, -1f), 0.001f)
    }

    @Test
    fun readableContourLineRotationYDegrees_keepsTextReadable() {
        val forward = readableContourLineRotationYDegrees(1f, 0f)
        assertTrue(forward in -90f..90f)
        val flipped = readableContourLineRotationYDegrees(-1f, 0f)
        assertTrue(flipped in -90f..90f)
    }
}
