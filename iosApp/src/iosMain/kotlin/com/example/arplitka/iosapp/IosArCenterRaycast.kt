package com.example.arplitka.iosapp

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.ARKit.ARHitTestResult
import platform.ARKit.ARHitTestResultTypeEstimatedHorizontalPlane
import platform.ARKit.ARHitTestResultTypeExistingPlaneUsingExtent
import platform.ARKit.ARPlaneAnchor
import platform.ARKit.ARSCNView
import platform.CoreGraphics.CGPointMake

internal enum class CenterHitSource {
    HIT_TEST
}

internal data class CenterPlaneHit(
    val confirmed: Boolean = false,
    val anchor: ARPlaneAnchor? = null,
    val localPoint: Pair<Float, Float>? = null,
    val confirmedHitResult: ARHitTestResult? = null,
    val previewHitResult: ARHitTestResult? = null,
    val confirmedSource: CenterHitSource? = null,
    val previewSource: CenterHitSource? = null
)

/**
 * Center screen hit via ARKit [ARSCNView.hitTest].
 * [ARRaycastQuery] is not wired in current Kotlin/Native ARKit bindings — use hitTest until cinterop adds it.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun ARSCNView.resolveCenterPlaneHit(): CenterPlaneHit {
    val width = bounds.useContents { size.width }
    val height = bounds.useContents { size.height }
    if (width <= 0.0 || height <= 0.0) return CenterPlaneHit()

    val center = CGPointMake(width / 2.0, height / 2.0)
    val existingHit = hitTest(center, ARHitTestResultTypeExistingPlaneUsingExtent)
        .firstHorizontalFloorHitResult()
    val existingAnchor = existingHit?.anchor as? ARPlaneAnchor
    val existingLocal = existingHit?.let { HitTransformReader.localFloorPoint(it) }
    val confirmed = existingAnchor != null &&
        existingLocal != null &&
        existingAnchor.containsLocalPoint(existingLocal.first, existingLocal.second)

    val estimatedHit = if (confirmed) {
        null
    } else {
        hitTest(center, ARHitTestResultTypeEstimatedHorizontalPlane)
            .firstEstimatedHorizontalFloorHitResult()
    }
    val estimatedAnchor = (estimatedHit?.anchor as? ARPlaneAnchor)
        ?.takeIf { it.isHorizontalTracking() }
    val estimatedLocal = estimatedHit?.let { HitTransformReader.localFloorPoint(it) }

    val visualAnchor = if (confirmed) existingAnchor else estimatedAnchor
    val visualLocal = if (confirmed) existingLocal else estimatedLocal

    return CenterPlaneHit(
        confirmed = confirmed,
        anchor = visualAnchor,
        localPoint = visualLocal,
        confirmedHitResult = if (confirmed) existingHit else null,
        previewHitResult = estimatedHit,
        confirmedSource = if (confirmed) CenterHitSource.HIT_TEST else null,
        previewSource = if (!confirmed && estimatedHit != null) CenterHitSource.HIT_TEST else null
    )
}

@OptIn(ExperimentalForeignApi::class)
internal fun CenterPlaneHit.placementWorldTransform() = confirmedHitResult?.worldTransform

@OptIn(ExperimentalForeignApi::class)
internal fun CenterPlaneHit.confirmedWorldFloorPoint() =
    confirmedHitResult?.let { HitTransformReader.worldFloorPoint(it) }

@OptIn(ExperimentalForeignApi::class)
internal fun CenterPlaneHit.previewWorldFloorPoint() =
    previewHitResult?.let { HitTransformReader.worldFloorPoint(it) }

internal fun CenterPlaneHit.hitPathDebugLabel(): String {
    val confirmedLabel = confirmedSource?.name?.lowercase() ?: "none"
    val previewLabel = previewSource?.name?.lowercase() ?: "none"
    return "c:$confirmedLabel/p:$previewLabel"
}
