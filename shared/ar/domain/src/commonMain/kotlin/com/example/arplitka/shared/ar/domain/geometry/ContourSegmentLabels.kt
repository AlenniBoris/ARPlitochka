package com.example.arplitka.shared.ar.domain.geometry

import com.example.arplitka.shared.ar.contracts.model.ArPoint3D
import com.example.arplitka.shared.ar.domain.model.FloorContourUiState
import com.example.arplitka.shared.ar.domain.model.PlacedContourPoint
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class ContourSegmentLabel(
    val key: String,
    val start: ArPoint3D,
    val end: ArPoint3D,
    val distanceMeters: Float,
    val midpointX: Float,
    val midpointY: Float,
    val midpointZ: Float,
    val rotationYDegrees: Float
)

const val CONTOUR_LABEL_WIDTH_M = 0.13f
const val CONTOUR_LABEL_HEIGHT_M = 0.045f
const val CONTOUR_LABEL_VISUAL_OFFSET_M = 0.018f
const val CONTOUR_POINT_RADIUS_M = 0.016f
const val CONTOUR_LINE_VISUAL_OFFSET_M = 0.003f

fun Float.formatContourDistanceMeters(): String {
    val rounded = (this * 100f).roundToInt() / 100f
    val whole = rounded.toInt()
    val fraction = ((rounded - whole) * 100f).roundToInt()
    return "${whole}.${fraction.toString().padStart(2, '0')} м"
}

fun buildContourSegmentLabels(
    state: FloorContourUiState,
    floorY: Float?
): List<ContourSegmentLabel> {
    if (!state.showContourLines || state.placedPoints.size < 2) return emptyList()

    val segments = mutableListOf<ContourSegmentLabel>()
    val points = state.placedPoints
    for (index in 0 until points.lastIndex) {
        segments += segmentLabel(
            key = "${points[index].id}-${points[index + 1].id}",
            startPoint = points[index],
            endPoint = points[index + 1],
            floorY = floorY
        )
    }
    if (state.isPolygonClosed && points.size >= 3) {
        segments += segmentLabel(
            key = "${points.last().id}-${points.first().id}-close",
            startPoint = points.last(),
            endPoint = points.first(),
            floorY = floorY
        )
    }
    return segments
}

private fun segmentLabel(
    key: String,
    startPoint: PlacedContourPoint,
    endPoint: PlacedContourPoint,
    floorY: Float?
): ContourSegmentLabel {
    val start = startPoint.position
    val end = endPoint.position
    val dx = end.xMeters - start.xMeters
    val dz = end.zMeters - start.zMeters
    val measuredLength = sqrt(dx * dx + dz * dz)
    val inset = CONTOUR_POINT_RADIUS_M
    val visualLength = measuredLength - inset - inset
    val y = (floorY ?: start.yMeters) + CONTOUR_LINE_VISUAL_OFFSET_M + CONTOUR_LABEL_VISUAL_OFFSET_M
    val (midX, midZ, rotationY) = if (visualLength > 0.001f && measuredLength > 0f) {
        val ux = dx / measuredLength
        val uz = dz / measuredLength
        val startX = start.xMeters + ux * inset
        val startZ = start.zMeters + uz * inset
        val endX = end.xMeters - ux * inset
        val endZ = end.zMeters - uz * inset
        Triple(
            (startX + endX) * 0.5f,
            (startZ + endZ) * 0.5f,
            contourLineRotationYDegrees(dx, dz)
        )
    } else {
        Triple(
            (start.xMeters + end.xMeters) * 0.5f,
            (start.zMeters + end.zMeters) * 0.5f,
            contourLineRotationYDegrees(dx, dz)
        )
    }
    return ContourSegmentLabel(
        key = key,
        start = start,
        end = end,
        distanceMeters = measuredLength,
        midpointX = midX,
        midpointY = y,
        midpointZ = midZ,
        rotationYDegrees = rotationY
    )
}

fun contourLineRotationYDegrees(dx: Float, dz: Float): Float =
    (-atan2(dz.toDouble(), dx.toDouble()) * 180.0 / PI).toFloat()

fun readableContourLineRotationYDegrees(dx: Float, dz: Float): Float {
    val angle = contourLineRotationYDegrees(dx, dz)
    return when {
        angle > 90f -> angle - 180f
        angle < -90f -> angle + 180f
        else -> angle
    }
}

fun contourDistanceLabelBatchKey(
    segments: List<ContourSegmentLabel>,
    floorY: Float?,
    showLines: Boolean
): Int {
    var key = segments.size * 31
    key = key * 31 + if (showLines) 1 else 0
    key = key * 31 + ((floorY ?: 0f) / 0.01f).roundToInt()
    segments.forEach { segment ->
        key = key * 31 + (segment.distanceMeters * 100f).roundToInt()
        key = key * 31 + (segment.midpointX * 100f).roundToInt()
        key = key * 31 + (segment.midpointZ * 100f).roundToInt()
        key = key * 31 + segment.rotationYDegrees.roundToInt()
    }
    return key
}
