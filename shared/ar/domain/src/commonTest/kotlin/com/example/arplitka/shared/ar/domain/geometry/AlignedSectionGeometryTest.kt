package com.example.arplitka.shared.ar.domain.geometry

import com.example.arplitka.shared.ar.contracts.model.ArPoint3D
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AlignedSectionGeometryTest {
    private fun point(x: Float, z: Float, y: Float = 0f) = ArPoint3D(x, y, z)

    private fun rectangle(width: Float, height: Float): List<ArPoint3D> = listOf(
        point(0f, 0f),
        point(width, 0f),
        point(width, height),
        point(0f, height)
    )

    private fun rotatePoints(points: List<ArPoint3D>, angleRadians: Float): List<ArPoint3D> {
        val cosA = cos(angleRadians)
        val sinA = sin(angleRadians)
        return points.map { point ->
            ArPoint3D(
                xMeters = point.xMeters * cosA - point.zMeters * sinA,
                yMeters = point.yMeters,
                zMeters = point.xMeters * sinA + point.zMeters * cosA
            )
        }
    }

    @Test
    fun axisAlignedRectangle_returnsLocalBoundsMatchingPhysicalSize() {
        val aligned = buildAlignedSectionGeometry(rectangle(width = 2f, height = 1f))

        assertEquals(2f, aligned.boundsWidthM, 0.01f)
        assertEquals(1f, aligned.boundsHeightM, 0.01f)
        assertEquals(0f, aligned.rotationYDegrees, 0.5f)
    }

    @Test
    fun rotatedRectangle_preservesLocalBoundsRegardlessOfWorldOrientation() {
        val rotated = rotatePoints(rectangle(width = 2f, height = 1f), angleRadians = PI.toFloat() / 6f)
        val aligned = buildAlignedSectionGeometry(rotated)

        assertEquals(2f, aligned.boundsWidthM, 0.05f)
        assertEquals(1f, aligned.boundsHeightM, 0.05f)
    }

    @Test
    fun longestEdgeAlongZ_setsRotationToNegativeNinetyDegrees() {
        val points = listOf(
            point(0f, 0f),
            point(0f, 2f),
            point(1f, 2f),
            point(1f, 0f)
        )
        val aligned = buildAlignedSectionGeometry(points)

        assertEquals(2f, aligned.boundsWidthM, 0.01f)
        assertEquals(1f, aligned.boundsHeightM, 0.01f)
        assertEquals(-90f, aligned.rotationYDegrees, 0.5f)
    }

    @Test
    fun localPoints_spanAlignedBounds() {
        val aligned = buildAlignedSectionGeometry(rectangle(width = 2f, height = 1f))
        val minX = aligned.localPoints.minOf { it.xMeters }
        val maxX = aligned.localPoints.maxOf { it.xMeters }
        val minY = aligned.localPoints.minOf { it.yMeters }
        val maxY = aligned.localPoints.maxOf { it.yMeters }

        assertEquals(aligned.boundsWidthM, maxX - minX, 0.01f)
        assertEquals(aligned.boundsHeightM, maxY - minY, 0.01f)
        assertEquals(0f, (minX + maxX) * 0.5f, 0.01f)
        assertEquals(0f, (minY + maxY) * 0.5f, 0.01f)
    }

    @Test
    fun centroid_matchesAverageWorldPosition() {
        val points = listOf(
            point(0f, 0f),
            point(4f, 0f),
            point(4f, 2f),
            point(0f, 2f)
        )
        val aligned = buildAlignedSectionGeometry(points)

        assertEquals(2f, aligned.centroidX, 0.01f)
        assertEquals(1f, aligned.centroidZ, 0.01f)
    }

    @Test
    fun worldShift_doesNotChangeLocalBounds() {
        val base = rectangle(width = 2f, height = 1f)
        val shifted = base.map { point(it.xMeters + 10f, it.zMeters + 5f, it.yMeters) }

        val baseAligned = buildAlignedSectionGeometry(base)
        val shiftedAligned = buildAlignedSectionGeometry(shifted)

        assertEquals(baseAligned.boundsWidthM, shiftedAligned.boundsWidthM, 0.01f)
        assertEquals(baseAligned.boundsHeightM, shiftedAligned.boundsHeightM, 0.01f)
        assertEquals(baseAligned.rotationYDegrees, shiftedAligned.rotationYDegrees, 0.5f)
    }

    @Test
    fun axisVectors_areOrthonormal() {
        val aligned = buildAlignedSectionGeometry(rectangle(width = 2f, height = 1f))
        val axisLength = sqrt(
            aligned.axisX * aligned.axisX + aligned.axisZ * aligned.axisZ
        )
        val perpendicularLength = sqrt(
            aligned.perpendicularX * aligned.perpendicularX +
                aligned.perpendicularZ * aligned.perpendicularZ
        )
        val dot = aligned.axisX * aligned.perpendicularX + aligned.axisZ * aligned.perpendicularZ

        assertEquals(1f, axisLength, 0.01f)
        assertEquals(1f, perpendicularLength, 0.01f)
        assertEquals(0f, dot, 0.01f)
    }

    private fun sqrt(value: Float): Float = kotlin.math.sqrt(value)
}
