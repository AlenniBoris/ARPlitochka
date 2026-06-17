package com.example.arplitka.iosapp.presentation.support

import com.example.arplitka.iosapp.platform.ar.CenterPlaneHit
import com.example.arplitka.iosapp.platform.ar.hitPathDebugLabel
import com.example.arplitka.shared.ar.contracts.model.ArPoint3D

internal enum class PlacementSnapshotSource {
    DELEGATE,
    RENDER_FLOOR,
    TAP_ACCEPTED;

    val sourceLabel: String
        get() = when (this) {
            DELEGATE -> "delegate"
            RENDER_FLOOR -> "renderFloor"
            TAP_ACCEPTED -> "tapAccepted"
        }
}

internal data class ResolvedPlacementSnapshot(
    val id: Long,
    val resolvedAtSeconds: Double,
    val sectionFloorY: Float?,
    val point: ArPoint3D?,
    val centerHit: CenterPlaneHit,
    val status: String,
    val hitPath: String,
    val source: PlacementSnapshotSource,
    val trackingDegraded: Boolean = false,
    val cameraGapMs: Int = 0
) {
    val sourceLabel: String
        get() = source.sourceLabel

    fun ageMs(nowSeconds: Double): Int = computeHitAgeMs(nowSeconds, resolvedAtSeconds)

    fun statusAt(nowSeconds: Double): String =
        resolvePlacementStatus(
            centerHit = centerHit,
            sectionFloorY = sectionFloorY,
            hitAgeMs = ageMs(nowSeconds)
        )
}

internal object PlacementSnapshotFactory {
    private var nextId: Long = 1L

    fun create(
        resolvedAtSeconds: Double,
        sectionFloorY: Float?,
        point: ArPoint3D?,
        centerHit: CenterPlaneHit,
        source: PlacementSnapshotSource,
        trackingDegraded: Boolean,
        cameraGapMs: Int,
        hitAgeMs: Int = 0
    ): ResolvedPlacementSnapshot =
        ResolvedPlacementSnapshot(
            id = nextId++,
            resolvedAtSeconds = resolvedAtSeconds,
            sectionFloorY = sectionFloorY,
            point = point,
            centerHit = centerHit,
            status = resolvePlacementStatus(centerHit, sectionFloorY, hitAgeMs),
            hitPath = centerHit.hitPathDebugLabel(),
            source = source,
            trackingDegraded = trackingDegraded,
            cameraGapMs = cameraGapMs
        )
}
