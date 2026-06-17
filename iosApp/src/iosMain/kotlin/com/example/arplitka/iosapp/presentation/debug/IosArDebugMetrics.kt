package com.example.arplitka.iosapp.presentation.debug

internal data class IosPlaneDebugMetrics(
    val sessionPhase: String = "scan",
    val perfDiagnosis: String = "warming up",
    /** [ARSessionDelegate.session:didUpdateFrame:] callbacks per second. */
    val sessionDelegateHz: Int = 0,
    /** Interval between consecutive ARKit [ARFrame.timestamp] values. */
    val cameraFrameGapMs: Int = 0,
    val cameraFrameGapLabel: String = "n/a",
    /** Wall-clock interval between didUpdateFrame callback entries. */
    val delegateWallGapMs: Int = 0,
    val delegateWallGapLabel: String = "n/a",
    /** Wall-clock time spent inside didUpdateFrame on the last frame. */
    val frameHandleMs: Int = 0,
    /** ARSCNView anchor node callbacks per second. */
    val rendererNodeCallbackHz: Int = 0,
    val rendererMode: String = "none",
    val overlayCount: Int = 0,
    val anchorLatencyMs: Int? = null,
    val sessionFeatures: String = "planes",
    val hitPath: String = "none",
    val detectGate: String = "searching",
    val scanPatch: String = "off",
    val placementStatus: String = "-",
    val hitAgeMs: Int = 0,
    val reticleHitAgeMs: Int = 0,
    val reticleHitAgeLabel: String = "n/a",
    val reticleSourceLabel: String = "none",
    /** Wall-clock age of live reticle hit at last tap attempt. */
    val tapFrameAgeMs: Int = 0,
    val tapFrameAgeLabel: String = "n/a",
    val tapSourceLabel: String = "-",
    val tapDeltaCm: Float = 0f,
    val tapDeltaLabel: String = "-",
    val trackingQualityLabel: String = "ok",
    val hitYLabel: String = "-",
    val largestPlaneAreaM2: Float = 0f,
    val relocLabel: String = "ok",
    val cullLabel: String = "d:0/e:0",
    val anchorCorrectionLabel: String = "n/a",
    val anchorRootDeltaCm: Float = 0f,
    val anchorDisplayDeltaCm: Float = 0f,
    val placementSnapshotId: Long = -1L,
    val placementSnapshotAgeMs: Int = 0,
    val placementSnapshotAgeLabel: String = "n/a",
    val tapSnapshotId: Long = -1L,
    val tapRejectReason: String = "-",
    val isPlacementPlaceable: Boolean = false,
    val contourVersion: Long = 0L,
    val contourSyncSource: String = "-",
    val manualAlignEligible: Boolean = false,
    val pendingCorrectionFrames: Int = 0
)
