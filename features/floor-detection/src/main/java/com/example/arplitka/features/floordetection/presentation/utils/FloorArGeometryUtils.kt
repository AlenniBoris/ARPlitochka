package com.example.arplitka.features.floordetection.presentation.utils

import com.example.arplitka.features.floordetection.domain.model.ArPoint
import io.github.sceneview.math.Position
import io.github.sceneview.math.Position2
import kotlin.math.atan2
import kotlin.math.sqrt

internal data class PolygonBounds(
    val width: Float,
    val height: Float
)

internal data class SectionGeometry(
    val polygonPath: List<Position2>,
    val rotationY: Float
)

internal data class SegmentGeometry(
    val measuredLength: Float,
    val visualLength: Float,
    val midPosition: Position,
    val dx: Float,
    val dz: Float,
    val rotationY: Float
)

internal fun List<ArPoint>.toAlignedSectionGeometry(
    centroidX: Float,
    centroidZ: Float
): SectionGeometry {
    val longestEdge = longestEdgeDirection()
    val ux = longestEdge.x
    val uz = longestEdge.z
    val perpendicularX = uz
    val perpendicularZ = -ux

    val polygonPath = map { point ->
        val dx = point.pose.tx() - centroidX
        val dz = point.pose.tz() - centroidZ

        Position2(
            x = dx * ux + dz * uz,
            y = dx * perpendicularX + dz * perpendicularZ
        )
    }

    return SectionGeometry(
        polygonPath = polygonPath,
        rotationY = -Math.toDegrees(atan2(uz, ux).toDouble()).toFloat()
    )
}

internal fun List<Position2>.bounds(): PolygonBounds {
    val minX = minOf { it.x }
    val maxX = maxOf { it.x }
    val minY = minOf { it.y }
    val maxY = maxOf { it.y }
    return PolygonBounds(
        width = maxX - minX,
        height = maxY - minY
    )
}

internal fun Float.roundToMillimeters(): Int {
    return (this * 1000f).toInt()
}

internal fun createSegmentGeometry(
    rawStart: Position,
    rawEnd: Position,
    y: Float,
    startInset: Float,
    endInset: Float
): SegmentGeometry? {
    val dx = rawEnd.x - rawStart.x
    val dz = rawEnd.z - rawStart.z
    val measuredLength = sqrt(dx * dx + dz * dz)
    val visualLength = measuredLength - startInset - endInset
    if (visualLength <= MIN_LINE_LENGTH_M) return null

    val ux = dx / measuredLength
    val uz = dz / measuredLength
    val start = Position(
        x = rawStart.x + ux * startInset,
        y = y,
        z = rawStart.z + uz * startInset
    )
    val end = Position(
        x = rawEnd.x - ux * endInset,
        y = y,
        z = rawEnd.z - uz * endInset
    )

    return SegmentGeometry(
        measuredLength = measuredLength,
        visualLength = visualLength,
        midPosition = Position(
            x = (start.x + end.x) / 2f,
            y = y,
            z = (start.z + end.z) / 2f
        ),
        dx = dx,
        dz = dz,
        rotationY = lineRotationYDegrees(dx, dz)
    )
}

internal fun readableLineRotationYDegrees(dx: Float, dz: Float): Float {
    val angle = lineRotationYDegrees(dx, dz)
    return when {
        angle > 90f -> angle - 180f
        angle < -90f -> angle + 180f
        else -> angle
    }
}

private data class EdgeDirection(
    val x: Float,
    val z: Float
)

private fun List<ArPoint>.longestEdgeDirection(): EdgeDirection {
    if (size < 2) return EdgeDirection(x = 1f, z = 0f)

    var longestLength = 0f
    var longestDx = 1f
    var longestDz = 0f

    indices.forEach { index ->
        val start = this[index].pose
        val end = this[(index + 1) % size].pose
        val dx = end.tx() - start.tx()
        val dz = end.tz() - start.tz()
        val length = sqrt(dx * dx + dz * dz)

        if (length > longestLength) {
            longestLength = length
            longestDx = dx
            longestDz = dz
        }
    }

    if (longestLength <= MIN_LINE_LENGTH_M) return EdgeDirection(x = 1f, z = 0f)

    return EdgeDirection(
        x = longestDx / longestLength,
        z = longestDz / longestLength
    )
}

private fun lineRotationYDegrees(dx: Float, dz: Float): Float {
    return -Math.toDegrees(atan2(dz, dx).toDouble()).toFloat()
}
