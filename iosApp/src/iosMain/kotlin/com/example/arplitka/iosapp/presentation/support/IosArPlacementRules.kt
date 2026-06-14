package com.example.arplitka.iosapp.presentation.support

import com.example.arplitka.iosapp.platform.ar.CenterPlaneHit
import com.example.arplitka.iosapp.platform.ar.confirmedWorldFloorPoint
import com.example.arplitka.iosapp.platform.ar.previewWorldFloorPoint
import com.example.arplitka.iosapp.platform.ar.IosArSessionRelocationController
import com.example.arplitka.shared.ar.contracts.model.ArPoint3D
import platform.CoreFoundation.CFAbsoluteTimeGetCurrent
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal const val METRICS_PUBLISH_INTERVAL_SECONDS = 0.25
internal const val SCAN_HIT_TEST_INTERVAL_FRAMES = 2
internal const val SCAN_OVERLAY_BUDGET_INTERVAL_FRAMES = 1
internal const val SCAN_OVERLAY_ELEVATION_INTERVAL_FRAMES = 2
internal const val SCAN_ANCHOR_NODE_UPDATE_INTERVAL = 3
internal const val PLACEMENT_MUTATION_COOLDOWN_FRAMES = 4
internal const val PLACEMENT_ANCHORED_POINTS_SYNC_INTERVAL_FRAMES = 6
/** Imperceptible anchor drift — applied immediately when tracking is stable. */
internal const val PLACEMENT_ANCHOR_MICRO_CORRECTION_M = 0.03f
/** Shifts above this never auto-apply; user confirms via the realign button. */
internal const val PLACEMENT_ANCHOR_MACRO_BLOCKED_M = 0.06f
/** Stable frames required before auto-applying a small (3–6 cm) post-freeze correction. */
internal const val PLACEMENT_ANCHOR_SMALL_AUTO_CONFIRM_FRAMES = 2
internal const val PLACEMENT_ANCHOR_CONFIRM_FRAMES = 3
internal const val PLACEMENT_ANCHOR_SIGNATURE_TOLERANCE_M = 0.04f
/** Legacy recovery window; instability latch below is the primary realign trigger. */
internal const val PLACEMENT_ANCHOR_RECOVERY_CONTEXT_SECONDS = 30.0
internal const val PLACEMENT_FLOOR_BAND_TOLERANCE_M = 0.10f
internal const val PLACEMENT_HIT_STALE_MS = 700
internal const val PLACEMENT_RENDER_RETICLE_MIN_INTERVAL_SECONDS = 1.0 / 120.0
internal const val PLACEMENT_PATCH_EURO_MIN_CUTOFF_HZ = 2.4
internal const val PLACEMENT_PATCH_EURO_BETA = 1.1
internal const val PLACEMENT_PATCH_EURO_D_CUTOFF_HZ = 3.5
internal const val PLACEMENT_PATCH_FAST_BLEND_START_MPS = 0.45f
internal const val PLACEMENT_PATCH_FAST_BLEND_FULL_MPS = 1.1f
internal const val PLACEMENT_PATCH_FAST_BLEND_MAX = 0.75f
internal const val PLACEMENT_RENDER_SMOOTHING_SNAP_M = 0.35f
internal const val LIVE_RETICLE_UPDATE_INTERVAL_SECONDS = 0.066
internal const val LIVE_RETICLE_UI_MAX_AGE_MS = 150
internal const val LIVE_RETICLE_TAP_MAX_AGE_MS = 150
internal const val TAP_MAX_PREVIEW_DELTA_CM = 5f
/** Debug threshold only: tap no longer blocks on delegate age. */
internal const val TAP_MAX_DELEGATE_AGE_MS = 150
internal const val TRACKING_DEGRADED_CAMERA_GAP_MS = 500
internal const val TAP_PLACEABLE_GRACE_SECONDS = 0.35
internal const val TAP_PLACEABLE_MAX_CAMERA_MOVE_M = 0.08f
internal const val TAP_PLACEABLE_MAX_UI_DELTA_M = 0.05f
internal val PLACEABLE_STATUSES = setOf("scan-valid", "valid", "preview")

internal fun currentTimeSeconds(): Double =
    CFAbsoluteTimeGetCurrent()

internal fun computeDelegateAgeMs(nowSeconds: Double, lastDelegateAtSeconds: Double): Int {
    if (lastDelegateAtSeconds <= 0.0) return Int.MAX_VALUE
    return ((nowSeconds - lastDelegateAtSeconds) * 1000.0)
        .roundToInt()
        .coerceAtLeast(0)
}

internal fun canClearTapRejectionHint(
    placementStatus: String,
    @Suppress("UNUSED_PARAMETER") nowSeconds: Double,
    @Suppress("UNUSED_PARAMETER") lastDelegateAtSeconds: Double
): Boolean = placementStatus in TAP_HINT_CLEAR_STATUSES

internal val TAP_HINT_CLEAR_STATUSES = setOf("valid", "preview", "scan-valid")

internal fun formatTapFrameAgeLabel(tapFrameAgeMs: Int, lastReticleAtSeconds: Double): String =
    when {
        lastReticleAtSeconds <= 0.0 -> "n/a"
        tapFrameAgeMs >= LIVE_RETICLE_TAP_MAX_AGE_MS -> "$tapFrameAgeMs ms (stale)"
        else -> "$tapFrameAgeMs ms"
    }

internal fun formatTapDeltaLabel(tapDeltaCm: Float): String =
    if (tapDeltaCm <= 0f) "-" else "${(tapDeltaCm * 10f).roundToInt() / 10.0} cm"

internal fun formatAnchorCorrectionLabel(
    stateLabel: String,
    rootDeltaCm: Float,
    displayDeltaCm: Float
): String {
    val rootPart = if (rootDeltaCm > 0f) "r:${(rootDeltaCm * 10f).roundToInt() / 10.0}" else null
    val displayPart = if (displayDeltaCm > 0f) "d:${(displayDeltaCm * 10f).roundToInt() / 10.0}" else null
    val deltas = listOfNotNull(rootPart, displayPart).joinToString(" ")
    return if (deltas.isEmpty()) stateLabel else "$stateLabel $deltas"
}

internal fun formatReticleHitAgeLabel(ageMs: Int): String =
    if (ageMs <= 0) "n/a" else "$ageMs ms"

internal fun formatTrackingQualityLabel(
    trackingDegraded: Boolean,
    overlayDebouncing: Boolean
): String = when {
    trackingDegraded && overlayDebouncing -> "degraded+debounce"
    trackingDegraded -> "degraded"
    overlayDebouncing -> "debounce"
    else -> "ok"
}

internal fun isTrackingDegraded(
    cameraGapMs: Int,
    relocationController: IosArSessionRelocationController
): Boolean =
    cameraGapMs >= TRACKING_DEGRADED_CAMERA_GAP_MS || relocationController.isRelocalizing()

/** Placement hint: ARKit tracking/relocalize only — not slow delegate frame interval. */
internal fun shouldShowPlacementCatchupHint(
    isRelocalizing: Boolean,
    isTrackingNormal: Boolean
): Boolean = isRelocalizing || !isTrackingNormal

internal fun horizontalArPointDistanceMeters(a: ArPoint3D, b: ArPoint3D): Float {
    val dx = a.xMeters - b.xMeters
    val dz = a.zMeters - b.zMeters
    return sqrt(dx * dx + dz * dz)
}

/**
 * Quick read for on-device perf triage (step 1 before continuous-floor work).
 * - camera gap: ARKit frame delivery
 * - delegate Hz: how often we finish didUpdateFrame
 * - frame work: last handler duration
 */
internal fun formatCameraFrameGapLabel(cameraGapMs: Int): String =
    when {
        cameraGapMs <= 0 -> "n/a"
        cameraGapMs >= 9_999 -> "n/a (need 2+ frames)"
        else -> "$cameraGapMs ms"
    }

internal fun diagnoseArPerf(
    delegateHz: Int,
    cameraGapMs: Int,
    frameHandleMs: Int
): String {
    if (cameraGapMs == 0 || cameraGapMs >= 9_999) return "warming up"
    val expectedDelegateHz = (1000f / cameraGapMs).roundToInt().coerceAtLeast(1)
    return when {
        frameHandleMs >= 250 -> "handler blocked (${frameHandleMs}ms)"
        cameraGapMs >= 250 && delegateHz <= 2 -> "camera sparse (~${1000 / cameraGapMs}hz)"
        delegateHz <= 2 && cameraGapMs <= 60 -> "delegate blocked (camera ok)"
        delegateHz < expectedDelegateHz / 2 && frameHandleMs >= 33 ->
            "delegate behind camera"
        frameHandleMs >= 50 -> "handler heavy (${frameHandleMs}ms)"
        else -> "ok (delegate ~${delegateHz}hz)"
    }
}

internal fun placementStatusLabel(
    centerHit: CenterPlaneHit,
    sectionFloorY: Float?,
    hitAgeMs: Int
): String {
    val hitY = centerHit.confirmedWorldFloorPoint()?.yMeters
        ?: centerHit.previewWorldFloorPoint()?.yMeters
    return when {
        sectionFloorY != null && hitAgeMs > LIVE_RETICLE_UI_MAX_AGE_MS -> "stale"
        sectionFloorY == null -> if (centerHit.confirmed) "scan-valid" else "scan-no-hit"
        hitY == null -> "no-hit"
        kotlin.math.abs(hitY - sectionFloorY) > PLACEMENT_FLOOR_BAND_TOLERANCE_M -> "height"
        centerHit.confirmed -> "valid"
        centerHit.previewWorldFloorPoint() != null -> "preview"
        else -> "no-hit"
    }
}

internal fun Float.formatMeters3(): String =
    "${(this * 1000f).roundToInt() / 1000.0}"

internal fun computeHitAgeMs(
    frameStartedAt: Double,
    lastResolvedAt: Double
): Int {
    if (lastResolvedAt <= 0.0) return 0
    return ((frameStartedAt - lastResolvedAt) * 1000.0)
        .roundToInt()
        .coerceIn(0, 60_000)
}

/**
 * One Euro Filter: stronger smoothing when the reticle is still, lighter lag when it moves quickly.
 */
internal class PlacementPatchAxisSmoother(
    private val minCutoffHz: Double = PLACEMENT_PATCH_EURO_MIN_CUTOFF_HZ,
    private val speedCoefficient: Double = PLACEMENT_PATCH_EURO_BETA,
    private val derivativeCutoffHz: Double = PLACEMENT_PATCH_EURO_D_CUTOFF_HZ
) {
    private var value: Double? = null
    private var derivative: Double = 0.0

    fun reset(value: Double) {
        this.value = value
        derivative = 0.0
    }

    fun filter(sample: Double, dt: Double): Double {
        if (dt <= 0.0) return sample
        val previous = value ?: return sample.also { value = it }
        val derivativeSample = (sample - previous) / dt
        derivative = smooth(derivativeSample, derivative, derivativeCutoffHz, dt)
        val cutoff = minCutoffHz + speedCoefficient * kotlin.math.abs(derivative)
        val filtered = smooth(sample, previous, cutoff, dt)
        value = filtered
        return filtered
    }

    private fun smooth(sample: Double, previous: Double, cutoffHz: Double, dt: Double): Double {
        val tau = 1.0 / (2.0 * kotlin.math.PI * cutoffHz)
        val alpha = 1.0 / (1.0 + tau / dt)
        return alpha * sample + (1.0 - alpha) * previous
    }
}

internal class PlacementPatchSmoother {
    private val xSmoother = PlacementPatchAxisSmoother()
    private val zSmoother = PlacementPatchAxisSmoother()

    fun reset(point: ArPoint3D) {
        xSmoother.reset(point.xMeters.toDouble())
        zSmoother.reset(point.zMeters.toDouble())
    }

    fun filter(rawPoint: ArPoint3D, dt: Double): ArPoint3D =
        ArPoint3D(
            xMeters = xSmoother.filter(rawPoint.xMeters.toDouble(), dt).toFloat(),
            yMeters = rawPoint.yMeters,
            zMeters = zSmoother.filter(rawPoint.zMeters.toDouble(), dt).toFloat()
        )
}

internal fun blendPlacementPatchForFastMotion(
    filtered: ArPoint3D,
    raw: ArPoint3D,
    previous: ArPoint3D,
    dt: Double
): ArPoint3D {
    if (dt <= 0.0) return filtered
    val dx = raw.xMeters - previous.xMeters
    val dz = raw.zMeters - previous.zMeters
    val rawSpeed = sqrt(dx * dx + dz * dz) / dt.toFloat()
    val span = PLACEMENT_PATCH_FAST_BLEND_FULL_MPS - PLACEMENT_PATCH_FAST_BLEND_START_MPS
    if (span <= 0f || rawSpeed <= PLACEMENT_PATCH_FAST_BLEND_START_MPS) {
        return filtered
    }
    val fastBlend = ((rawSpeed - PLACEMENT_PATCH_FAST_BLEND_START_MPS) / span)
        .coerceIn(0f, 1f) * PLACEMENT_PATCH_FAST_BLEND_MAX
    return ArPoint3D(
        xMeters = filtered.xMeters + (raw.xMeters - filtered.xMeters) * fastBlend,
        yMeters = raw.yMeters,
        zMeters = filtered.zMeters + (raw.zMeters - filtered.zMeters) * fastBlend
    )
}

