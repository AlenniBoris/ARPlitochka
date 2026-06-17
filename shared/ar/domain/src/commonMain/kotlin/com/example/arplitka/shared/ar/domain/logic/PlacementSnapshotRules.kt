package com.example.arplitka.shared.ar.domain.logic

object PlacementSnapshotRules {
    const val LIVE_RETICLE_UI_MAX_AGE_MS = 150
    /** Tap/add may use the last accepted snapshot within this window (includes user reaction time). */
    const val LIVE_RETICLE_TAP_MAX_AGE_MS = 350
    const val TRACKING_DEGRADED_CAMERA_GAP_MS = 500

    val PLACEABLE_STATUSES = setOf("scan-valid", "valid", "preview")

    /** Snapshot is still fresh if either wall-clock age has not expired yet. */
    fun isSnapshotAgeFresh(ageMs: Int, delegateAgeMs: Int): Boolean =
        ageMs <= LIVE_RETICLE_TAP_MAX_AGE_MS || delegateAgeMs <= LIVE_RETICLE_TAP_MAX_AGE_MS

    fun isSnapshotAgeExpired(ageMs: Int, delegateAgeMs: Int): Boolean =
        !isSnapshotAgeFresh(ageMs, delegateAgeMs)

    /**
     * Stale when the accepted snapshot itself is old or ARKit is relocalizing.
     * A single large inter-frame camera gap after a stall must not permanently block a
     * freshly published snapshot — sustained degradation is handled via [isTrackingDegraded].
     */
    fun isSnapshotStale(
        ageMs: Int,
        delegateAgeMs: Int,
        isRelocalizing: Boolean,
        trackingDegraded: Boolean,
        status: String
    ): Boolean =
        status == "stale" ||
            isRelocalizing ||
            isSnapshotAgeExpired(ageMs, delegateAgeMs) ||
            trackingDegraded

    /**
     * Tracking is degraded only when relocalizing or both the delegate and snapshot have
     * been stale long enough (sustained starvation, not a one-off recovery frame).
     */
    fun isTrackingDegraded(
        cameraGapMs: Int,
        snapshotAgeMs: Int,
        delegateAgeMs: Int,
        isRelocalizing: Boolean
    ): Boolean =
        isRelocalizing ||
            (
                cameraGapMs >= TRACKING_DEGRADED_CAMERA_GAP_MS &&
                    isSnapshotAgeExpired(snapshotAgeMs, delegateAgeMs)
                )

    fun isStatusPlaceable(status: String, hasPoint: Boolean): Boolean =
        hasPoint && status in PLACEABLE_STATUSES
}
