package com.example.arplitka.shared.ar.core

import androidx.compose.ui.unit.IntSize
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState

fun Plane.isUsableHorizontalPlane(): Boolean {
    return type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
            trackingState == TrackingState.TRACKING
}

fun Plane.area(): Float = extentX * extentZ

fun Frame.centerPlaneHit(viewportSize: IntSize): HitResult? {
    if (viewportSize.width <= 0 || viewportSize.height <= 0) return null
    val hits = hitTest(viewportSize.width / 2f, viewportSize.height / 2f)
    for (hit in hits) {
        val plane = hit.trackable as? Plane ?: continue
        if (plane.isUsableHorizontalPlane() && plane.isPoseInPolygon(hit.hitPose)) {
            return hit
        }
    }
    return null
}
