package com.example.arplitka.shared.ar.domain.geometry

import com.example.arplitka.shared.ar.contracts.model.ArPoint2D
import com.example.arplitka.shared.ar.contracts.model.ArPoint3D
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

private const val MIN_EDGE_LENGTH_M = 0.001f

data class AlignedSectionGeometry(
    val localPoints: List<ArPoint2D>,
    val centroidX: Float,
    val centroidZ: Float,
    val axisX: Float,
    val axisZ: Float,
    val perpendicularX: Float,
    val perpendicularZ: Float,
    val rotationYDegrees: Float,
    val boundsWidthM: Float,
    val boundsHeightM: Float
)

fun buildAlignedSectionGeometry(points: List<ArPoint3D>): AlignedSectionGeometry {
    require(points.size >= 3) { "At least 3 points are required for aligned section geometry" }

    val centroidX = points.map { it.xMeters }.average().toFloat()
    val centroidZ = points.map { it.zMeters }.average().toFloat()
    val longestEdge = longestEdgeDirection(points)
    val axisX = longestEdge.x
    val axisZ = longestEdge.z
    val perpendicularX = axisZ
    val perpendicularZ = -axisX

    val localPoints = points.map { point ->
        val dx = point.xMeters - centroidX
        val dz = point.zMeters - centroidZ
        ArPoint2D(
            xMeters = dx * axisX + dz * axisZ,
            yMeters = dx * perpendicularX + dz * perpendicularZ
        )
    }

    val boundsWidthM = (localPoints.maxOf { it.xMeters } - localPoints.minOf { it.xMeters })
        .coerceAtLeast(MIN_EDGE_LENGTH_M)
    val boundsHeightM = (localPoints.maxOf { it.yMeters } - localPoints.minOf { it.yMeters })
        .coerceAtLeast(MIN_EDGE_LENGTH_M)

    return AlignedSectionGeometry(
        localPoints = localPoints,
        centroidX = centroidX,
        centroidZ = centroidZ,
        axisX = axisX,
        axisZ = axisZ,
        perpendicularX = perpendicularX,
        perpendicularZ = perpendicularZ,
        rotationYDegrees = sectionRotationYDegrees(axisX, axisZ),
        boundsWidthM = boundsWidthM,
        boundsHeightM = boundsHeightM
    )
}

private data class EdgeDirection(
    val x: Float,
    val z: Float
)

private fun longestEdgeDirection(points: List<ArPoint3D>): EdgeDirection {
    if (points.size < 2) return EdgeDirection(x = 1f, z = 0f)

    var longestLength = 0f
    var longestDx = 1f
    var longestDz = 0f

    points.indices.forEach { index ->
        val start = points[index]
        val end = points[(index + 1) % points.size]
        val dx = end.xMeters - start.xMeters
        val dz = end.zMeters - start.zMeters
        val length = sqrt(dx * dx + dz * dz)

        if (length > longestLength) {
            longestLength = length
            longestDx = dx
            longestDz = dz
        }
    }

    if (longestLength <= MIN_EDGE_LENGTH_M) return EdgeDirection(x = 1f, z = 0f)

    return EdgeDirection(
        x = longestDx / longestLength,
        z = longestDz / longestLength
    )
}

private fun sectionRotationYDegrees(axisX: Float, axisZ: Float): Float =
    (-atan2(axisZ.toDouble(), axisX.toDouble()) * 180.0 / PI).toFloat()
