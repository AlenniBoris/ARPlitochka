package com.example.arplitka.shared.ar.domain.logic

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlacementSnapshotRulesTest {
    @Test
    fun isSnapshotStale_trueWhenStatusStale() {
        assertTrue(
            PlacementSnapshotRules.isSnapshotStale(
                ageMs = 0,
                delegateAgeMs = 0,
                isRelocalizing = false,
                trackingDegraded = false,
                status = "stale"
            )
        )
    }

    @Test
    fun isSnapshotAgeExpired_trueWhenBothAgesExceedTapLimit() {
        assertTrue(
            PlacementSnapshotRules.isSnapshotAgeExpired(
                ageMs = PlacementSnapshotRules.LIVE_RETICLE_TAP_MAX_AGE_MS + 1,
                delegateAgeMs = PlacementSnapshotRules.LIVE_RETICLE_TAP_MAX_AGE_MS + 1
            )
        )
    }

    @Test
    fun isSnapshotAgeFresh_trueWhenDelegateRecentlyRan() {
        assertTrue(
            PlacementSnapshotRules.isSnapshotAgeFresh(
                ageMs = PlacementSnapshotRules.LIVE_RETICLE_TAP_MAX_AGE_MS + 50,
                delegateAgeMs = 20
            )
        )
    }

    @Test
    fun isSnapshotStale_falseWhenOnlySnapshotAgeExpiredButDelegateFresh() {
        assertFalse(
            PlacementSnapshotRules.isSnapshotStale(
                ageMs = PlacementSnapshotRules.LIVE_RETICLE_TAP_MAX_AGE_MS + 50,
                delegateAgeMs = 20,
                isRelocalizing = false,
                trackingDegraded = false,
                status = "scan-valid"
            )
        )
    }

    @Test
    fun isSnapshotStale_trueWhenRelocalizing() {
        assertTrue(
            PlacementSnapshotRules.isSnapshotStale(
                ageMs = 0,
                delegateAgeMs = 0,
                isRelocalizing = true,
                trackingDegraded = false,
                status = "valid"
            )
        )
    }

    @Test
    fun isSnapshotStale_falseForFreshSnapshotAfterLargeCameraGap() {
        assertFalse(
            PlacementSnapshotRules.isSnapshotStale(
                ageMs = 20,
                delegateAgeMs = 20,
                isRelocalizing = false,
                trackingDegraded = false,
                status = "scan-valid"
            )
        )
    }

    @Test
    fun isTrackingDegraded_falseForFreshSnapshotDespiteLargeCameraGap() {
        assertFalse(
            PlacementSnapshotRules.isTrackingDegraded(
                cameraGapMs = 9_847,
                snapshotAgeMs = 20,
                delegateAgeMs = 20,
                isRelocalizing = false
            )
        )
    }

    @Test
    fun isTrackingDegraded_trueWhenCameraGapAndBothAgesExpired() {
        assertTrue(
            PlacementSnapshotRules.isTrackingDegraded(
                cameraGapMs = 9847,
                snapshotAgeMs = PlacementSnapshotRules.LIVE_RETICLE_TAP_MAX_AGE_MS + 1,
                delegateAgeMs = PlacementSnapshotRules.LIVE_RETICLE_TAP_MAX_AGE_MS + 1,
                isRelocalizing = false
            )
        )
    }

    @Test
    fun isStatusPlaceable_acceptsPreviewAndScanValid() {
        assertTrue(PlacementSnapshotRules.isStatusPlaceable("preview", hasPoint = true))
        assertTrue(PlacementSnapshotRules.isStatusPlaceable("scan-valid", hasPoint = true))
        assertFalse(PlacementSnapshotRules.isStatusPlaceable("stale", hasPoint = true))
        assertFalse(PlacementSnapshotRules.isStatusPlaceable("valid", hasPoint = false))
    }
}
