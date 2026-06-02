package com.example.arplitka.shared.ar.domain.logic

import com.example.arplitka.shared.ar.contracts.model.ArPoint3D
import kotlin.math.abs
import kotlin.math.sqrt

object FloorGeometry {
    const val CLOSE_THRESHOLD_M = 0.10f
    const val SNAP_THRESHOLD_M = 0.05f
    const val MAX_POINT_HEIGHT_DELTA_M = 0.08f

    fun distance(a: ArPoint3D, b: ArPoint3D): Float {
        val dx = a.xMeters - b.xMeters
        val dy = a.yMeters - b.yMeters
        val dz = a.zMeters - b.zMeters
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun isWithinHeightTolerance(point: ArPoint3D, floorY: Float): Boolean =
        abs(point.yMeters - floorY) <= MAX_POINT_HEIGHT_DELTA_M
}
