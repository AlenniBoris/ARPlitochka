package com.example.arplitka.iosapp.platform.ar

import com.example.arplitka.iosapp.bridge.pg_session_add_anchor_from_column_major
import com.example.arplitka.iosapp.platform.render.HitTransformReader
import com.example.arplitka.iosapp.platform.render.bridgePointer
import com.example.arplitka.iosapp.presentation.support.PLACEMENT_ANCHOR_CONFIRM_FRAMES
import com.example.arplitka.iosapp.presentation.support.PLACEMENT_ANCHOR_MACRO_BLOCKED_M
import com.example.arplitka.iosapp.presentation.support.PLACEMENT_ANCHOR_MICRO_CORRECTION_M
import com.example.arplitka.iosapp.presentation.support.PLACEMENT_ANCHOR_SIGNATURE_TOLERANCE_M
import com.example.arplitka.iosapp.presentation.support.PLACEMENT_ANCHOR_SMALL_AUTO_CONFIRM_FRAMES
import com.example.arplitka.shared.ar.contracts.model.ArPoint3D
import com.example.arplitka.shared.ar.domain.logic.FloorGeometry
import com.example.arplitka.shared.ar.domain.model.PlacedContourPoint
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.ARKit.ARAnchor
import platform.ARKit.ARFrame
import platform.ARKit.ARSession
import platform.Foundation.NSUUID
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal data class AnchorCorrectionDebug(
    val stateLabel: String,
    val rootDeltaCm: Float,
    val displayDeltaCm: Float,
    val pendingCorrectionFrames: Int = 0,
    val manualAlignEligible: Boolean = false
)

@OptIn(ExperimentalForeignApi::class)
internal class IosFloorAnchorStore {
    internal data class Entry(
        val logicalId: String,
        /** Fallback only. The stable source of truth is local XZ on [contourRootAnchorId]. */
        val tapWorldPosition: ArPoint3D,
        val rootLocalX: Float?,
        val rootLocalZ: Float?,
        var lastResolvedWorldPosition: ArPoint3D
    )

    private val entries = mutableListOf<Entry>()
    private var contourRootAnchorId: NSUUID? = null
    private var lastContourRootAnchor: ARAnchor? = null
    private var lastAcceptedRootX: Float? = null
    private var lastAcceptedRootZ: Float? = null
    private var pendingCorrectionSignature: Int? = null
    private var pendingCorrectionFrames: Int = 0
    private var lastAcceptedFloorY: Float? = null
    private var macroCorrectionBlocked: Boolean = false
    private var manualRealignLatched: Boolean = false
    private var pendingMacroPositions: List<ArPoint3D>? = null
    private var pendingMacroRootOrigin: ArPoint3D? = null
    private var correctionStateLabel: String = "stable"
    private var lastRootDeltaCm: Float = 0f
    private var lastDisplayDeltaCm: Float = 0f

    val logicalIds: List<String>
        get() = entries.map { it.logicalId }

    fun setSectionPlaneAnchorId(anchorId: NSUUID?) {
        // Kept for coordinator compatibility.
    }

    fun register(
        session: ARSession,
        logicalId: String,
        worldPosition: ArPoint3D,
        frame: ARFrame?
    ) {
        val rootAnchor = if (contourRootAnchorId == null) {
            createContourRootAnchor(session, worldPosition)?.also { anchor ->
                contourRootAnchorId = anchor.identifier
                lastContourRootAnchor = anchor
            }
        } else {
            findContourRootAnchor(frame)
        }
        val rootLocal = rootAnchor?.let { anchor ->
            HitTransformReader.worldXZOnAnchor(anchor, worldPosition)
        }
        entries += Entry(
            logicalId = logicalId,
            tapWorldPosition = worldPosition,
            rootLocalX = rootLocal?.first,
            rootLocalZ = rootLocal?.second,
            lastResolvedWorldPosition = worldPosition
        )
        lastAcceptedFloorY = worldPosition.yMeters
        val origin = rootOrigin(rootAnchor)
        if (origin != null) {
            lastAcceptedRootX = origin.xMeters
            lastAcceptedRootZ = origin.zMeters
            // CRITICAL: When registering a new point, commit current anchor state for all points
            // to ensure they are all in the same coordinate space immediately.
            val currentResolved = entries.map { entry ->
                rootAnchor?.let { resolveEntryWorldPosition(entry, it) } ?: entry.lastResolvedWorldPosition
            }
            commitAcceptedPositions(currentResolved, origin)
        }
        resetPendingCorrection()
        clearManualRealignState()
    }

    fun detachLast(session: ARSession, frame: ARFrame?) {
        entries.removeLastOrNull()
        if (entries.isEmpty()) {
            removeContourRootAnchor(session, frame)
        }
    }

    fun detachAll(session: ARSession, frame: ARFrame?) {
        entries.clear()
        removeContourRootAnchor(session, frame)
    }

    fun hasBlockedMacroCorrection(): Boolean = macroCorrectionBlocked

    fun canManuallyRealign(): Boolean =
        entries.isNotEmpty() && (
            manualRealignLatched ||
                lastDisplayDeltaCm >= PLACEMENT_ANCHOR_MACRO_BLOCKED_M * 100f ||
                lastRootDeltaCm >= PLACEMENT_ANCHOR_MACRO_BLOCKED_M * 100f ||
                correctionStateLabel == "offer-realign" ||
                correctionStateLabel == "pending-macro"
            )

    fun applyManualMacroRealignment(frame: ARFrame?): Boolean {
        if (entries.isEmpty()) return false
        val pendingPositions = pendingMacroPositions
        val pendingRootOrigin = pendingMacroRootOrigin
        if (pendingPositions != null && pendingRootOrigin != null) {
            commitAcceptedPositions(pendingPositions, pendingRootOrigin)
            clearManualRealignState()
            correctionStateLabel = "manual"
            lastRootDeltaCm = 0f
            lastDisplayDeltaCm = 0f
            return true
        }
        val rootAnchor = findContourRootAnchorInFrame(frame) ?: return false
        val rootOrigin = rootOrigin(rootAnchor) ?: return false
        val resolvedPositions = entries.map { entry ->
            resolveEntryWorldPosition(entry, rootAnchor)
        }
        val maxDisplayDelta = maxDisplayDeltaMeters(resolvedPositions)
        val acceptedRootX = lastAcceptedRootX ?: return false
        val acceptedRootZ = lastAcceptedRootZ ?: return false
        val rootDelta = horizontalDistance(
            rootOrigin.xMeters,
            rootOrigin.zMeters,
            acceptedRootX,
            acceptedRootZ
        )
        if (max(rootDelta, maxDisplayDelta) < PLACEMENT_ANCHOR_MACRO_BLOCKED_M) return false
        commitAcceptedPositions(resolvedPositions, rootOrigin)
        clearManualRealignState()
        correctionStateLabel = "manual"
        lastRootDeltaCm = 0f
        lastDisplayDeltaCm = 0f
        return true
    }

    fun correctionDebug(): AnchorCorrectionDebug =
        AnchorCorrectionDebug(
            stateLabel = correctionStateLabel,
            rootDeltaCm = lastRootDeltaCm,
            displayDeltaCm = lastDisplayDeltaCm,
            pendingCorrectionFrames = pendingCorrectionFrames,
            manualAlignEligible = canManuallyRealign()
        )

    fun placedPoints(
        frame: ARFrame?,
        sectionFloorY: Float?,
        trackingStable: Boolean = true,
        hadTrackingInstability: Boolean = false,
        isFinalized: Boolean = false
    ): List<PlacedContourPoint> {
        if (entries.isEmpty()) return emptyList()
        
        val rootAnchor = findContourRootAnchorInFrame(frame)
        val resolvedPositions = if (rootAnchor != null) {
            entries.map { entry ->
                resolveEntryWorldPosition(entry, rootAnchor)
            }
        } else {
            entries.map { it.lastResolvedWorldPosition }
        }

        // We ALWAYS evaluate correction to update metrics (deltas) and trigger the realign button,
        // even if we don't use the corrected positions for rendering during placement.
        val acceptedPositions = evaluateAnchorCorrection(
            resolvedPositions = resolvedPositions,
            rootAnchor = rootAnchor,
            trackingStable = trackingStable,
            hadTrackingInstability = hadTrackingInstability,
            anchorResolvedFromFrame = rootAnchor != null
        )

        // PURE WORLD SPACE FOR PLACEMENT:
        // To ensure 100% stability while drawing, we return the original tap positions.
        // We only switch to anchor-relative positions after finalization.
        if (!isFinalized) {
            return entries.map { entry ->
                PlacedContourPoint(id = entry.logicalId, position = entry.tapWorldPosition)
            }
        }

        val effectiveFloorY = lastAcceptedFloorY ?: sectionFloorY
        return entries.zip(acceptedPositions).map { (entry, resolved) ->
            val position = FloorGeometry.projectToSectionFloor(resolved, effectiveFloorY)
            PlacedContourPoint(id = entry.logicalId, position = position)
        }
    }

    fun anchoredFloorY(frame: ARFrame?): Float? =
        lastAcceptedFloorY ?: anchoredFloorY(findContourRootAnchor(frame))

    fun entriesInternal() = entries
    fun resolveEntryWorldPositionInternal(entry: Entry, rootAnchor: ARAnchor?) = resolveEntryWorldPosition(entry, rootAnchor)
    fun findContourRootAnchor(frame: ARFrame?): ARAnchor? = findContourRootAnchorInFrame(frame) ?: lastContourRootAnchor

    private fun createContourRootAnchor(session: ARSession, worldPosition: ArPoint3D): ARAnchor? {
        val transform = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            worldPosition.xMeters,
            worldPosition.yMeters,
            worldPosition.zMeters,
            1f
        )
        return transform.usePinned { pinned ->
            val anchorPtr = pg_session_add_anchor_from_column_major(
                sessionPtr = session.bridgePointer(),
                in16 = pinned.addressOf(0)
            )
            anchorPtr as? ARAnchor
        }
    }

    private fun resolveEntryWorldPosition(entry: Entry, rootAnchor: ARAnchor?): ArPoint3D {
        val localX = entry.rootLocalX
        val localZ = entry.rootLocalZ
        if (rootAnchor == null || localX == null || localZ == null) {
            return entry.lastResolvedWorldPosition
        }
        return HitTransformReader.worldPointFromPlaneLocal(
            anchor = rootAnchor,
            localX = localX,
            localZ = localZ,
            localY = 0f
        ) ?: entry.lastResolvedWorldPosition
    }

    private fun evaluateAnchorCorrection(
        resolvedPositions: List<ArPoint3D>,
        rootAnchor: ARAnchor?,
        trackingStable: Boolean,
        hadTrackingInstability: Boolean,
        anchorResolvedFromFrame: Boolean
    ): List<ArPoint3D> {
        if (resolvedPositions.size != entries.size) {
            resetPendingCorrection()
            clearManualRealignState()
            correctionStateLabel = "frozen"
            return entries.map { it.lastResolvedWorldPosition }
        }

        if (!anchorResolvedFromFrame || rootAnchor == null) {
            correctionStateLabel = "no-frame-anchor"
            return entries.map { it.lastResolvedWorldPosition }
        }

        val rootOrigin = rootOrigin(rootAnchor)
        if (rootOrigin == null) {
            correctionStateLabel = "no-root"
            lastRootDeltaCm = 0f
            lastDisplayDeltaCm = 0f
            return entries.map { it.lastResolvedWorldPosition }
        }

        val acceptedRootX = lastAcceptedRootX
        val acceptedRootZ = lastAcceptedRootZ
        if (acceptedRootX == null || acceptedRootZ == null) {
            commitAcceptedPositions(resolvedPositions, rootOrigin)
            correctionStateLabel = "init"
            lastRootDeltaCm = 0f
            lastDisplayDeltaCm = 0f
            clearManualRealignState()
            return resolvedPositions
        }

        val rootDelta = horizontalDistance(
            rootOrigin.xMeters,
            rootOrigin.zMeters,
            acceptedRootX,
            acceptedRootZ
        )
        val maxDisplayDelta = maxDisplayDeltaMeters(resolvedPositions)
        val correctionDelta = max(rootDelta, maxDisplayDelta)
        lastRootDeltaCm = rootDelta * 100f
        lastDisplayDeltaCm = maxDisplayDelta * 100f

        // DEAD GRIP STRATEGY: 
        // If we have a stable anchor, we follow it 100% without any "micro-corrections"
        // that cause jitter. We only block updates if tracking is unstable and drift is huge.
        if (!trackingStable) {
            if (correctionDelta >= PLACEMENT_ANCHOR_MACRO_BLOCKED_M) {
                storePendingMacroCandidate(resolvedPositions, rootOrigin)
            }
            maybeOfferManualRealign(
                resolvedPositions = resolvedPositions,
                rootOrigin = rootOrigin,
                correctionDelta = correctionDelta,
                hadTrackingInstability = hadTrackingInstability,
                forceOffer = true
            )
            correctionStateLabel = if (manualRealignLatched) {
                "offer-realign"
            } else {
                "frozen-unstable"
            }
            return entries.map { it.lastResolvedWorldPosition }
        }

        when {
            correctionDelta >= PLACEMENT_ANCHOR_MACRO_BLOCKED_M -> {
                storePendingMacroCandidate(resolvedPositions, rootOrigin)
                accumulatePendingCorrection(resolvedPositions)
                val persistent = pendingCorrectionFrames >= PLACEMENT_ANCHOR_CONFIRM_FRAMES
                maybeOfferManualRealign(
                    resolvedPositions = resolvedPositions,
                    rootOrigin = rootOrigin,
                    correctionDelta = correctionDelta,
                    hadTrackingInstability = hadTrackingInstability,
                    persistent = persistent
                )
                correctionStateLabel = if (manualRealignLatched) {
                    "offer-realign"
                } else {
                    "pending-macro"
                }
                return entries.map { it.lastResolvedWorldPosition }
            }
            else -> {
                // WORLD-LOCKED DEAD GRIP:
                // For points placement, we keep world positions rock-solid to avoid jitter.
                // We only sync to the anchor in register() when a new point is added,
                // or if the user finalizes the area.
                correctionStateLabel = "world-locked"
                return entries.map { it.lastResolvedWorldPosition }
            }
        }
    }

    private fun maybeOfferManualRealign(
        resolvedPositions: List<ArPoint3D>,
        rootOrigin: ArPoint3D,
        correctionDelta: Float,
        hadTrackingInstability: Boolean,
        persistent: Boolean = false,
        forceOffer: Boolean = false
    ) {
        if (!forceOffer && !hadTrackingInstability && !persistent) return
        if (correctionDelta < PLACEMENT_ANCHOR_MACRO_BLOCKED_M) return
        storePendingMacroCandidate(resolvedPositions, rootOrigin)
        manualRealignLatched = true
        macroCorrectionBlocked = true
    }

    private fun maxDisplayDeltaMeters(resolvedPositions: List<ArPoint3D>): Float =
        entries.zip(resolvedPositions).maxOfOrNull { (entry, resolved) ->
            horizontalDistance(
                resolved.xMeters,
                resolved.zMeters,
                entry.lastResolvedWorldPosition.xMeters,
                entry.lastResolvedWorldPosition.zMeters
            )
        } ?: 0f

    private fun max(a: Float, b: Float): Float = if (a >= b) a else b

    private fun storePendingMacroCandidate(
        resolvedPositions: List<ArPoint3D>,
        rootOrigin: ArPoint3D
    ) {
        pendingMacroPositions = resolvedPositions
        pendingMacroRootOrigin = rootOrigin
    }

    private fun clearManualRealignState() {
        macroCorrectionBlocked = false
        manualRealignLatched = false
        pendingMacroPositions = null
        pendingMacroRootOrigin = null
        correctionStateLabel = "stable"
        lastRootDeltaCm = 0f
        lastDisplayDeltaCm = 0f
    }

    private fun commitAcceptedPositions(
        resolvedPositions: List<ArPoint3D>,
        rootOrigin: ArPoint3D
    ) {
        entries.zip(resolvedPositions).forEach { (entry, resolved) ->
            entry.lastResolvedWorldPosition = resolved
        }
        lastAcceptedRootX = rootOrigin.xMeters
        lastAcceptedRootZ = rootOrigin.zMeters
        lastAcceptedFloorY = resolvedPositions.firstOrNull()?.yMeters ?: lastAcceptedFloorY
    }

    private fun accumulatePendingCorrection(resolvedPositions: List<ArPoint3D>) {
        val signature = correctionSignature(resolvedPositions)
        pendingCorrectionFrames = if (pendingCorrectionSignature == signature) {
            pendingCorrectionFrames + 1
        } else {
            pendingCorrectionSignature = signature
            1
        }
    }

    private fun resetPendingCorrection() {
        pendingCorrectionSignature = null
        pendingCorrectionFrames = 0
    }

    private fun correctionSignature(points: List<ArPoint3D>): Int {
        val first = points.firstOrNull() ?: return 0
        val bucketX = (first.xMeters / PLACEMENT_ANCHOR_SIGNATURE_TOLERANCE_M).roundToInt()
        val bucketZ = (first.zMeters / PLACEMENT_ANCHOR_SIGNATURE_TOLERANCE_M).roundToInt()
        return bucketX * 31 + bucketZ
    }

    private fun horizontalDistance(ax: Float, az: Float, bx: Float, bz: Float): Float {
        val dx = ax - bx
        val dz = az - bz
        return sqrt(dx * dx + dz * dz)
    }

    fun rootOrigin(frame: ARFrame?): ArPoint3D? =
        rootOrigin(findContourRootAnchor(frame))

    private fun rootOrigin(rootAnchor: ARAnchor?): ArPoint3D? =
        rootAnchor?.let { anchor ->
            HitTransformReader.worldPointFromPlaneLocal(
                anchor = anchor,
                localX = 0f,
                localZ = 0f,
                localY = 0f
            )
        }

    private fun anchoredFloorY(rootAnchor: ARAnchor?): Float? =
        rootOrigin(rootAnchor)?.yMeters

    private fun findContourRootAnchorInFrame(frame: ARFrame?): ARAnchor? {
        val rootAnchorId = contourRootAnchorId ?: return null
        return frame?.findAnchor(rootAnchorId)?.also { anchor ->
            lastContourRootAnchor = anchor
        }
    }

    private fun removeContourRootAnchor(session: ARSession, frame: ARFrame?) {
        val rootAnchorId = contourRootAnchorId ?: return
        frame?.findAnchor(rootAnchorId)?.let { session.removeAnchor(it) }
        contourRootAnchorId = null
        lastContourRootAnchor = null
        lastAcceptedRootX = null
        lastAcceptedRootZ = null
        lastAcceptedFloorY = null
        resetPendingCorrection()
        clearManualRealignState()
    }

    private fun ARFrame.findAnchor(anchorId: NSUUID): ARAnchor? {
        return anchors.firstOrNull { anchor ->
            (anchor as? ARAnchor)?.identifier?.isEqual(anchorId) == true
        } as? ARAnchor
    }
}
