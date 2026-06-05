package com.example.arplitka.iosapp

import kotlinx.cinterop.ExperimentalForeignApi
import platform.ARKit.ARCamera
import platform.ARKit.ARFrame
import platform.ARKit.ARPlaneAnchor
import platform.ARKit.ARSession
import platform.ARKit.ARSessionRunOptionRemoveExistingAnchors
import platform.ARKit.ARSessionRunOptionResetTracking
import platform.ARKit.ARTrackingState
import platform.ARKit.ARTrackingStateReason
import platform.CoreFoundation.CFAbsoluteTimeGetCurrent
import kotlin.math.sqrt

internal const val RELOCATION_RESET_COOLDOWN_S = 3.0
internal const val DISTANT_PLANE_RESET_RATIO = 0.6f
internal const val DISTANT_PLANE_RESET_MIN_COUNT = 5

@OptIn(ExperimentalForeignApi::class)
internal class IosArSessionRelocationController {
    private var lastResetSeconds: Double = 0.0
    private var relocLabel: String = "ok"
    private var wasInterrupted: Boolean = false

    fun relocLabel(): String = relocLabel

    fun reset() {
        lastResetSeconds = 0.0
        relocLabel = "ok"
        wasInterrupted = false
    }

    fun onSessionInterrupted() {
        wasInterrupted = true
        relocLabel = "interrupted"
    }

    fun onInterruptionEnded(): Boolean {
        if (!wasInterrupted) return false
        wasInterrupted = false
        return true
    }

    fun onTrackingStateChanged(camera: ARCamera): RelocationResetRequest? {
        if (camera.trackingState == ARTrackingState.ARTrackingStateLimited &&
            camera.trackingStateReason == ARTrackingStateReason.ARTrackingStateReasonRelocalizing
        ) {
            relocLabel = "relocalizing"
            return RelocationResetRequest(reason = "reloc")
        }
        if (camera.trackingState == ARTrackingState.ARTrackingStateNormal) {
            if (relocLabel == "relocalizing") {
                relocLabel = "ok"
            }
        }
        return null
    }

    fun distantPlaneResetRequest(frame: ARFrame): RelocationResetRequest? {
        val camera = HitTransformReader.worldPointFromCamera(frame.camera)
        val horizontal = frame.anchors
            .mapNotNull { it as? ARPlaneAnchor }
            .filter { it.isHorizontalTracking() }
        if (horizontal.size < DISTANT_PLANE_RESET_MIN_COUNT) return null
        val distant = horizontal.count { anchor ->
            horizontalDistanceMeters(camera, HitTransformReader.worldPointFromAnchor(anchor)) >
                MAX_OVERLAY_DISTANCE_M
        }
        if (distant < DISTANT_PLANE_RESET_MIN_COUNT) return null
        if (distant.toFloat() / horizontal.size < DISTANT_PLANE_RESET_RATIO) return null
        return RelocationResetRequest(reason = "distant")
    }

    fun markResetPerformed(reason: String) {
        lastResetSeconds = CFAbsoluteTimeGetCurrent()
        relocLabel = reason
    }

    fun canResetNow(): Boolean =
        CFAbsoluteTimeGetCurrent() - lastResetSeconds >= RELOCATION_RESET_COOLDOWN_S
}

internal data class RelocationResetRequest(
    val reason: String
)

internal data class ScanSessionResetCallbacks(
    val onRendererReset: () -> Unit,
    val onFocusReset: () -> Unit,
    val onHitCacheReset: () -> Unit,
    val onOverlayLatencyReset: () -> Unit
)

@OptIn(ExperimentalForeignApi::class)
internal fun performScanSessionReset(
    session: ARSession,
    request: RelocationResetRequest,
    relocationController: IosArSessionRelocationController,
    callbacks: ScanSessionResetCallbacks,
    force: Boolean = false
) {
    if (!force && !relocationController.canResetNow()) return
    val (configuration, _) = createWorldTrackingConfiguration(enableLidarMesh = true)
    session.runWithConfiguration(
        configuration,
        ARSessionRunOptionResetTracking or ARSessionRunOptionRemoveExistingAnchors
    )
    callbacks.onRendererReset()
    callbacks.onFocusReset()
    callbacks.onHitCacheReset()
    callbacks.onOverlayLatencyReset()
    relocationController.markResetPerformed(request.reason)
}

private fun horizontalDistanceMeters(
    camera: com.example.arplitka.shared.ar.contracts.model.ArPoint3D,
    anchor: com.example.arplitka.shared.ar.contracts.model.ArPoint3D
): Float {
    val dx = anchor.xMeters - camera.xMeters
    val dz = anchor.zMeters - camera.zMeters
    return sqrt(dx * dx + dz * dz)
}
