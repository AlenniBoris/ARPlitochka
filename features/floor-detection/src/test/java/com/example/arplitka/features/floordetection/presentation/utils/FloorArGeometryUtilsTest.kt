package com.example.arplitka.features.floordetection.presentation.utils

import io.github.sceneview.math.Position
import io.github.sceneview.math.Position2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FloorArGeometryUtilsTest {

    @Test
    fun `roundToMillimeters rounds correctly`() {
        assertEquals(1500, 1.5004f.roundToMillimeters())
        assertEquals(1501, 1.5006f.roundToMillimeters())
    }

    @Test
    fun `bounds calculates correct width and height`() {
        val points = listOf(
            Position2(0f, 0f),
            Position2(1f, 0f),
            Position2(1f, 2f),
            Position2(0f, 2f)
        )
        val bounds = points.bounds()
        assertEquals(1f, bounds.width)
        assertEquals(2f, bounds.height)
    }

    @Test
    fun `createSegmentGeometry returns null for short segments`() {
        val start = Position(0f, 0f, 0f)
        val end = Position(0.01f, 0f, 0f) // 1cm, less than MIN_LINE_LENGTH_M
        val segment = createSegmentGeometry(start, end, 0f, 0f, 0f)
        assertNull(segment)
    }

    @Test
    fun `createSegmentGeometry calculates correct length and position`() {
        val start = Position(0f, 0f, 0f)
        val end = Position(1f, 0f, 0f)
        val segment = createSegmentGeometry(start, end, 0f, 0f, 0f)
        
        assertNotNull(segment)
        assertEquals(1f, segment!!.measuredLength, 0.001f)
        assertEquals(0.5f, segment.midPosition.x, 0.001f)
        assertEquals(0f, segment.midPosition.z, 0.001f)
    }

    @Test
    fun `readableLineRotationYDegrees returns correct angle`() {
        // dx=1, dz=0 -> angle 0
        assertEquals(0f, readableLineRotationYDegrees(1f, 0f), 0.001f)
        // dx=0, dz=1 -> angle -90
        assertEquals(-90f, readableLineRotationYDegrees(0f, 1f), 0.001f)
    }
}
