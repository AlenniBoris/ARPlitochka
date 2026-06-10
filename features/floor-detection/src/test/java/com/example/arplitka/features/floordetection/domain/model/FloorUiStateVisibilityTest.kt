package com.example.arplitka.features.floordetection.domain.model

import com.google.ar.core.Anchor
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FloorUiStateVisibilityTest {

    private fun point(): ArPoint = ArPoint(anchor = mockk(relaxed = true), pose = Pose.IDENTITY)

    @Test
    fun `placement with two points shows lines and points but not fill`() {
        val state = FloorUiState(
            points = listOf(point(), point()),
            isPolygonClosed = false,
            isContourConfirmed = false
        )
        assertTrue(state.showContourPoints)
        assertTrue(state.showContourLines)
        assertFalse(state.showSectionFill)
    }

    @Test
    fun `closed before confirm shows fill points and lines`() {
        val state = FloorUiState(
            points = listOf(point(), point(), point()),
            isPolygonClosed = true,
            isContourConfirmed = false
        )
        assertTrue(state.showContourPoints)
        assertTrue(state.showContourLines)
        assertTrue(state.showSectionFill)
    }

    @Test
    fun `confirmed without tile keeps points lines and fill`() {
        val state = FloorUiState(
            points = listOf(point(), point(), point()),
            isPolygonClosed = true,
            isContourConfirmed = true,
            isTileVisible = false
        )
        assertTrue(state.showContourPoints)
        assertTrue(state.showContourLines)
        assertTrue(state.showSectionFill)
        assertFalse(state.showPlaneRenderer)
    }

    @Test
    fun `tile mode hides contour geometry`() {
        val state = FloorUiState(
            points = listOf(point(), point(), point()),
            isPolygonClosed = true,
            isContourConfirmed = true,
            isTileVisible = true
        )
        assertFalse(state.showContourPoints)
        assertFalse(state.showContourLines)
        assertTrue(state.showSectionFill)
    }

    @Test
    fun `tracking fields do not affect visibility flags`() {
        val state = FloorUiState(
            trackingState = TrackingState.TRACKING,
            points = listOf(point(), point()),
            isPolygonClosed = false
        )
        assertTrue(state.showContourLines)
    }
}
