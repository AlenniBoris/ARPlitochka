@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.arplitka.iosapp

import com.example.arplitka.iosapp.bridge.PG_RAYCAST_ESTIMATED_PLANE
import com.example.arplitka.iosapp.bridge.PG_RAYCAST_EXISTING_PLANE
import com.example.arplitka.iosapp.bridge.PG_RAYCAST_MESH
import com.example.arplitka.iosapp.bridge.pg_center_raycast
import com.example.arplitka.iosapp.bridge.pg_session_supports_mesh_raycast
import com.example.arplitka.shared.ar.contracts.model.ArPoint3D
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CValue
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.useContents
import kotlinx.cinterop.value
import platform.ARKit.ARFrame
import platform.ARKit.ARHitTestResult
import platform.ARKit.ARHitTestResultTypeEstimatedHorizontalPlane
import platform.ARKit.ARHitTestResultTypeExistingPlaneUsingExtent
import platform.ARKit.ARPlaneAnchor
import platform.ARKit.ARSCNView
import platform.ARKit.ARSession
import platform.ARKit.ARTrackingState
import platform.CoreGraphics.CGPointMake
import platform.SceneKit.SCNMatrix4
import platform.SceneKit.SCNMatrix4FromMat4

internal enum class CenterHitSource {
    RAYCAST_EXISTING,
    RAYCAST_ESTIMATED,
    RAYCAST_MESH,
    HIT_TEST
}

internal typealias ArWorldScnMatrix = CValue<SCNMatrix4>

internal data class RaycastHitSample(
    val worldFloorPoint: ArPoint3D,
    val worldScnMatrix: ArWorldScnMatrix? = null,
    val anchor: ARPlaneAnchor? = null,
    val distance: Float = 0f,
    val localPoint: Pair<Float, Float>? = null,
    val source: CenterHitSource,
    val legacyHitResult: ARHitTestResult? = null
)

internal data class CenterPlaneHit(
    val confirmed: Boolean = false,
    val anchor: ARPlaneAnchor? = null,
    val localPoint: Pair<Float, Float>? = null,
    val confirmedSample: RaycastHitSample? = null,
    val previewSample: RaycastHitSample? = null,
    val confirmedHitResult: ARHitTestResult? = null,
    val previewHitResult: ARHitTestResult? = null,
    val confirmedSource: CenterHitSource? = null,
    val previewSource: CenterHitSource? = null
)

/**
 * Center screen hit via ARKit [ARRaycast] (B.1) with legacy hitTest fallback.
 * Mesh raycast (B.5) is used only when plane raycasts miss and LiDAR mesh is enabled.
 * SceneKit matrix comes from hitTest only — K/N cannot construct [SCNMatrix4] from floats.
 */
internal fun ARSCNView.resolveCenterPlaneHit(
    frame: ARFrame? = null,
    session: ARSession? = null
): CenterPlaneHit {
    val width = bounds.useContents { size.width }
    val height = bounds.useContents { size.height }
    if (width <= 0.0 || height <= 0.0) return CenterPlaneHit()

    val centerX = (width / 2.0).toFloat()
    val centerY = (height / 2.0).toFloat()
    val activeSession = session ?: this.session
    val activeFrame = frame ?: activeSession.currentFrame

    val legacyExistingHit = hitTestCenterResult(
        view = this,
        frame = activeFrame,
        centerX = centerX,
        centerY = centerY,
        estimated = false
    )
    val existingSample = raycastCenterSample(
        session = activeSession,
        frame = activeFrame,
        screenX = centerX,
        screenY = centerY,
        targetKind = PG_RAYCAST_EXISTING_PLANE,
        source = CenterHitSource.RAYCAST_EXISTING
    )?.mergeWithHit(legacyExistingHit)
        ?: legacyExistingHit?.toSample(CenterHitSource.HIT_TEST)

    val existingAnchor = existingSample?.anchor
    val existingLocal = existingSample?.localPoint
    val confirmed = existingAnchor != null &&
        existingLocal != null &&
        existingAnchor.containsLocalPoint(existingLocal.first, existingLocal.second)

    val legacyEstimatedHit = hitTestCenterResult(
        view = this,
        frame = activeFrame,
        centerX = centerX,
        centerY = centerY,
        estimated = true
    )
    val estimatedSample = raycastCenterSample(
        session = activeSession,
        frame = activeFrame,
        screenX = centerX,
        screenY = centerY,
        targetKind = PG_RAYCAST_ESTIMATED_PLANE,
        source = CenterHitSource.RAYCAST_ESTIMATED
    )?.mergeWithHit(legacyEstimatedHit)
        ?: legacyEstimatedHit?.toSample(CenterHitSource.HIT_TEST)

    val meshSample = if (confirmedSampleMissing(confirmed, estimatedSample) && meshRaycastEnabled(activeSession)) {
        raycastCenterSample(
            session = activeSession,
            frame = activeFrame,
            screenX = centerX,
            screenY = centerY,
            targetKind = PG_RAYCAST_MESH,
            source = CenterHitSource.RAYCAST_MESH
        )?.mergeWithHit(legacyEstimatedHit)
    } else {
        null
    }

    val previewSample = estimatedSample ?: meshSample
    val visualAnchor = if (confirmed) existingAnchor else previewSample?.anchor
    val visualLocal = if (confirmed) existingLocal else previewSample?.localPoint

    return CenterPlaneHit(
        confirmed = confirmed,
        anchor = visualAnchor,
        localPoint = visualLocal,
        confirmedSample = if (confirmed) existingSample else null,
        previewSample = previewSample,
        confirmedHitResult = if (confirmed) legacyExistingHit else null,
        previewHitResult = legacyEstimatedHit,
        confirmedSource = if (confirmed) existingSample?.source else null,
        previewSource = previewSample?.source
    )
}

internal fun CenterPlaneHit.confirmedWorldScnMatrix(): ArWorldScnMatrix? =
    confirmedSample?.worldScnMatrix ?: confirmedHitResult?.let { SCNMatrix4FromMat4(it.worldTransform) }

internal fun CenterPlaneHit.previewWorldScnMatrix(): ArWorldScnMatrix? =
    previewSample?.worldScnMatrix ?: previewHitResult?.let { SCNMatrix4FromMat4(it.worldTransform) }

/** Placement uses ARKit hit transform when available (required by [ARAnchor]). */
internal fun CenterPlaneHit.placementWorldTransform() = confirmedHitResult?.worldTransform

internal fun CenterPlaneHit.placementWorldFloorPoint() = confirmedWorldFloorPoint()

internal fun CenterPlaneHit.confirmedWorldFloorPoint(): ArPoint3D? =
    confirmedSample?.worldFloorPoint ?: confirmedHitResult?.let { HitTransformReader.worldFloorPoint(it) }

internal fun CenterPlaneHit.previewWorldFloorPoint(): ArPoint3D? =
    previewSample?.worldFloorPoint ?: previewHitResult?.let { HitTransformReader.worldFloorPoint(it) }

internal fun CenterPlaneHit.hitPathDebugLabel(): String {
    val confirmedLabel = confirmedSource?.debugToken() ?: if (confirmed) "yes" else "no"
    val previewLabel = previewSource?.debugToken() ?: if (previewSample != null || previewHitResult != null) "est" else "no"
    return "c:$confirmedLabel/p:$previewLabel"
}

internal fun ARFrame.isTrackingNormal(): Boolean =
    camera.trackingState == ARTrackingState.ARTrackingStateNormal

/** B.3: weak tracking — only stable polygon overlays, no estimated scan content. */
internal fun ARFrame.strictOutdoorScanMode(): Boolean =
    camera.trackingState != ARTrackingState.ARTrackingStateNormal

internal fun ARFrame.cameraWorldYMeters(): Float =
    HitTransformReader.worldPointFromCamera(camera).yMeters

internal fun ARFrame.cameraWorldPosition(): ArPoint3D =
    HitTransformReader.worldPointFromCamera(camera)

private fun RaycastHitSample.mergeWithHit(hit: ARHitTestResult?): RaycastHitSample {
    if (hit == null) return this
    return copy(
        worldScnMatrix = SCNMatrix4FromMat4(hit.worldTransform),
        worldFloorPoint = HitTransformReader.worldFloorPoint(hit),
        anchor = anchor ?: (hit.anchor as? ARPlaneAnchor),
        localPoint = localPoint ?: HitTransformReader.localFloorPoint(hit),
        legacyHitResult = hit
    )
}

private fun ARHitTestResult.toSample(source: CenterHitSource): RaycastHitSample =
    RaycastHitSample(
        worldFloorPoint = HitTransformReader.worldFloorPoint(this),
        worldScnMatrix = SCNMatrix4FromMat4(worldTransform),
        anchor = anchor as? ARPlaneAnchor,
        distance = distance.toFloat(),
        localPoint = HitTransformReader.localFloorPoint(this),
        source = source,
        legacyHitResult = this
    )

private fun confirmedSampleMissing(
    confirmed: Boolean,
    estimatedSample: RaycastHitSample?
): Boolean = !confirmed && estimatedSample == null

private fun meshRaycastEnabled(session: ARSession): Boolean =
    pg_session_supports_mesh_raycast(session.bridgePointer())

private fun raycastCenterSample(
    session: ARSession?,
    frame: ARFrame?,
    screenX: Float,
    screenY: Float,
    targetKind: Int,
    source: CenterHitSource
): RaycastHitSample? {
    if (session == null || frame == null) return null
    return memScoped {
        val distanceVar = alloc<FloatVar>()
        val transformValues = allocArray<FloatVar>(16)
        val anchorUuid = allocArray<ByteVar>(64)
        val found = pg_center_raycast(
            sessionPtr = session.bridgePointer(),
            framePtr = frame.bridgePointer(),
            screenX = screenX,
            screenY = screenY,
            targetKind = targetKind,
            outDistance = distanceVar.ptr,
            outWorldTransform16 = transformValues,
            outAnchorUuid = anchorUuid,
            outAnchorUuidCapacity = 64
        )
        if (!found) return@memScoped null

        val worldFloorPoint = ArPoint3D(
            xMeters = transformValues[12],
            yMeters = transformValues[13],
            zMeters = transformValues[14]
        )
        val uuidString = anchorUuid.toKString()
        val anchor = frame.findPlaneAnchor(uuidString)
        val localPoint = anchor?.let { planeAnchor ->
            HitTransformReader.worldXZOnAnchor(planeAnchor, worldFloorPoint)
        }
        RaycastHitSample(
            worldFloorPoint = worldFloorPoint,
            anchor = anchor,
            distance = distanceVar.value,
            localPoint = localPoint,
            source = source
        )
    }
}

private fun hitTestCenterResult(
    view: ARSCNView,
    frame: ARFrame?,
    centerX: Float,
    centerY: Float,
    estimated: Boolean
): ARHitTestResult? {
    val center = CGPointMake(centerX.toDouble(), centerY.toDouble())
    return if (estimated) {
        frame?.hitTest(center, ARHitTestResultTypeEstimatedHorizontalPlane)
            ?.closestEstimatedHorizontalFloorHitResult()
            ?: view.hitTest(center, ARHitTestResultTypeEstimatedHorizontalPlane)
                .closestEstimatedHorizontalFloorHitResult()
    } else {
        frame?.hitTest(center, ARHitTestResultTypeExistingPlaneUsingExtent)
            ?.closestHorizontalFloorHitResult()
            ?: view.hitTest(center, ARHitTestResultTypeExistingPlaneUsingExtent)
                .closestHorizontalFloorHitResult()
    }
}

private fun ARFrame.findPlaneAnchor(uuidString: String): ARPlaneAnchor? {
    if (uuidString.isEmpty()) return null
    return anchors
        .mapNotNull { it as? ARPlaneAnchor }
        .firstOrNull { it.identifier.UUIDString() == uuidString }
}

private fun CenterHitSource.debugToken(): String = when (this) {
    CenterHitSource.RAYCAST_EXISTING -> "ray"
    CenterHitSource.RAYCAST_ESTIMATED -> "est-ray"
    CenterHitSource.RAYCAST_MESH -> "mesh"
    CenterHitSource.HIT_TEST -> "hit"
}
