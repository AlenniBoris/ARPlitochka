package com.example.arplitka.shared.ar.contracts.model

data class ArPoint2D(
    val xMeters: Float,
    val yMeters: Float
)

data class ArPoint3D(
    val xMeters: Float,
    val yMeters: Float,
    val zMeters: Float
)

data class ArSurfacePolygon(
    val points: List<ArPoint2D>
) {
    val isClosed: Boolean = points.size >= MIN_POLYGON_POINTS

    companion object {
        private const val MIN_POLYGON_POINTS = 3
    }
}

data class ArTileTexture(
    val textureUrl: String,
    val repeatWidthMm: Int,
    val repeatLengthMm: Int,
    val rotationDegrees: Float
)

enum class ArTrackingStatus {
    INITIALIZING,
    SEARCHING_FLOOR,
    FLOOR_DETECTED,
    TRACKING_LOST,
    POLYGON_CLOSED,
    FINALIZED
}

enum class ArInstruction {
    PLEASE_WAIT,
    SEARCHING,
    MOVE_PHONE,
    DETECTED,
    CONTOUR_CLOSED,
    CONTOUR_CONFIRMED,
    TILE_VISIBLE,
    EMPTY
}
