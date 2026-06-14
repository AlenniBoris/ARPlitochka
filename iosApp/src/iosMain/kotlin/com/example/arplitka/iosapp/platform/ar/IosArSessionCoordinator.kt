package com.example.arplitka.iosapp.platform.ar

import com.example.arplitka.iosapp.presentation.debug.IosPlaneDebugMetrics
import com.example.arplitka.iosapp.presentation.mapper.toIosHint
import com.example.arplitka.iosapp.presentation.support.*
import com.example.arplitka.iosapp.platform.render.FocusedPlaneTracker
import com.example.arplitka.iosapp.platform.render.GridSurfaceMode
import com.example.arplitka.iosapp.platform.render.IosArContourRenderer
import com.example.arplitka.iosapp.platform.render.IosArPlaneSurfaceRenderer
import com.example.arplitka.iosapp.platform.render.MIN_FLOOR_AREA_M2
import com.example.arplitka.iosapp.platform.render.IosArScanSurfaceContext
import com.example.arplitka.iosapp.platform.render.shouldShowPlacementPatch
import com.example.arplitka.iosapp.platform.render.suppressArkitPlaneMesh
import com.example.arplitka.iosapp.platform.ar.cameraWorldPosition
import com.example.arplitka.iosapp.platform.ar.cameraWorldYMeters
import com.example.arplitka.iosapp.platform.ar.isTrackingNormal
import com.example.arplitka.iosapp.platform.ar.strictOutdoorScanMode
import com.example.arplitka.iosapp.platform.render.isHorizontalTracking
import com.example.arplitka.iosapp.platform.render.placementFloorPoint
import com.example.arplitka.shared.ar.contracts.model.ArPoint3D
import com.example.arplitka.shared.ar.contracts.state.FloorArEvent
import com.example.arplitka.shared.ar.domain.FloorArController
import com.example.arplitka.shared.ar.domain.FloorArEffect
import com.example.arplitka.shared.ar.domain.logic.AddPointValidation
import com.example.arplitka.shared.ar.domain.logic.FloorContourReducer
import com.example.arplitka.shared.ar.domain.logic.FloorGeometry
import com.example.arplitka.shared.ar.domain.logic.FloorSnapReducer
import com.example.arplitka.shared.ar.domain.model.FloorContourUiState
import com.example.arplitka.shared.ar.domain.model.FloorFrameSnapshot
import com.example.arplitka.shared.ar.domain.model.PlacedContourPoint
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import platform.ARKit.*
import platform.Foundation.NSUUID
import platform.SceneKit.SCNNode
import platform.SceneKit.SCNSceneRendererProtocol
import platform.darwin.NSObject
import kotlin.math.roundToInt

@OptIn(ExperimentalForeignApi::class)
internal class IosArSessionCoordinator(
    private val floorArController: FloorArController,
    private val onTrackingNameChanged: (String) -> Unit,
    private val onPlaneDebugMetricsChanged: (IosPlaneDebugMetrics) -> Unit,
    private val onPlacementHintChanged: (String?) -> Unit,
    private val onContourRealignAvailableChanged: (Boolean) -> Unit
) : NSObject(), ARSCNViewDelegateProtocol, ARSessionDelegateProtocol {

    private var sceneView: ARSCNView? = null
    private var isSessionTracking = false
    private var sessionFeatures: Set<IosArSessionFeature> = emptySet()
    private val focusedPlaneTracker = FocusedPlaneTracker()
    private val anchorStore = IosFloorAnchorStore()
    private val contourRenderer = IosArContourRenderer()
    private val planeSurfaceRenderer = IosArPlaneSurfaceRenderer()
    private val relocationController = IosArSessionRelocationController()
    private var cachedCenterHit: CenterPlaneHit = CenterPlaneHit()
    private var lastCenterHit: CenterPlaneHit = CenterPlaneHit()
    private var lastCenterHitResolvedSeconds: Double = 0.0
    private var hitTestFrameCounter: Int = 0
    private var placementMutationCooldownFrames: Int = 0
    private var workingFloorY: Float? = null
    private var workingFloorAreaM2: Float = 0f
    private var sessionStartTimeSeconds: Double = 0.0
    private var delegateHzWindowStartSeconds: Double = 0.0
    private var delegateHzFrameCount: Int = 0
    private var sessionDelegateHz: Int = 0
    private var lastArFrameTimestamp: Double? = null
    private var cameraFrameGapMs: Int = 0
    private var delegateWallGapMs: Int = 0
    private var lastDelegateAtSeconds: Double = 0.0
    private var liveReticleHit: CenterPlaneHit = CenterPlaneHit()
    private var liveReticlePoint: ArPoint3D? = null
    private var smoothedPlacementPatchPoint: ArPoint3D? = null
    private val placementPatchSmoother = PlacementPatchSmoother()
    private var liveReticleResolvedSeconds: Double = 0.0
    private var liveReticleSourceLabel: String = "none"
    private var lastTapFrameAgeMs: Int = 0
    private var lastTapDeltaCm: Float = 0f
    private var lastTapSourceLabel: String = "-"
    private var lastFrameHandleMs: Int = 0
    private var lastMetricsPublishSeconds: Double = 0.0
    private var lastRendererMode: String = "none"
    private var firstSurfaceOverlayMs: Int? = null
    private var placementFreezeApplied: Boolean = false
    private var placementUiFrameCounter: Int = 0
    private var lastPlacementRenderReticleSeconds: Double = 0.0
    private var lastFinalizedContourRenderSyncSeconds: Double = 0.0
    private var lastPlacementPatchSmoothSeconds: Double = 0.0
    private var scanOverlaySyncCounter: Int = 0
    private var scanAnchorNodeUpdateCounter: Int = 0
    private var rendererNodeCallbackWindowStartSeconds: Double = 0.0
    private var rendererNodeCallbackCount: Int = 0
    private var rendererNodeCallbackHz: Int = 0
    private var placementAnchorTrackingWasUnstable: Boolean = false
    private var placementAnchorHadInstability: Boolean = false
    private var placementAnchorRecoveryContextUntilSeconds: Double = 0.0

    fun attach(sceneView: ARSCNView) {
        val now = currentTimeSeconds()
        this.sceneView = sceneView
        focusedPlaneTracker.reset()
        relocationController.reset()
        planeSurfaceRenderer.reset()
        planeSurfaceRenderer.prepare()
        hitTestFrameCounter = 0
        placementMutationCooldownFrames = 0
        workingFloorY = null
        workingFloorAreaM2 = 0f
        cachedCenterHit = CenterPlaneHit()
        lastCenterHit = CenterPlaneHit()
        lastCenterHitResolvedSeconds = 0.0
        isSessionTracking = false
        sessionStartTimeSeconds = now
        delegateHzWindowStartSeconds = now
        delegateHzFrameCount = 0
        sessionDelegateHz = 0
        lastArFrameTimestamp = null
        cameraFrameGapMs = 0
        delegateWallGapMs = 0
        lastDelegateAtSeconds = 0.0
        clearLiveReticleHit()
        lastTapFrameAgeMs = 0
        lastTapDeltaCm = 0f
        lastTapSourceLabel = "-"
        lastFrameHandleMs = 0
        lastMetricsPublishSeconds = 0.0
        lastRendererMode = "none"
        firstSurfaceOverlayMs = null
        placementFreezeApplied = false
        placementUiFrameCounter = 0
        lastPlacementRenderReticleSeconds = 0.0
        lastPlacementPatchSmoothSeconds = 0.0
        placementAnchorTrackingWasUnstable = false
        placementAnchorHadInstability = false
        placementAnchorRecoveryContextUntilSeconds = 0.0
        scanOverlaySyncCounter = 0
        scanAnchorNodeUpdateCounter = 0
        rendererNodeCallbackWindowStartSeconds = now
        rendererNodeCallbackCount = 0
        rendererNodeCallbackHz = 0
        sceneView.debugOptions = 0UL
        sceneView.delegate = this
        sceneView.session.delegate = this
        sceneView.scene?.rootNode?.let { contourRenderer.attach(it) }
        val (configuration, features) = createWorldTrackingConfiguration(
            enableLidarMesh = supportsSceneReconstructionMesh()
        )
        sessionFeatures = features
        sceneView.session.runWithConfiguration(
            configuration,
            ARSessionRunOptionResetTracking or ARSessionRunOptionRemoveExistingAnchors
        )
    }

    fun pause() {
        sceneView?.session?.let { session ->
            anchorStore.detachAll(session, session.currentFrame)
        }
        contourRenderer.detach()
        planeSurfaceRenderer.reset()
        sceneView?.session?.pause()
        sceneView?.delegate = null
        sceneView?.session?.delegate = null
        focusedPlaneTracker.reset()
        relocationController.reset()
        sceneView = null
    }

    fun rescanSession() {
        val view = sceneView ?: return
        if (floorArController.currentState().placedPoints.isNotEmpty()) {
            dispatchEvent(FloorArEvent.Reset)
        } else {
            onPlacementHintChanged(null)
        }
        performScanReset(
            session = view.session,
            request = RelocationResetRequest(reason = "manual"),
            force = true
        )
        publishScanResetUiSnapshot()
    }

    fun applyContourRealignment() {
        val view = sceneView ?: return
        val frame = view.session.currentFrame ?: return
        if (!anchorStore.applyManualMacroRealignment(frame)) return
        placementAnchorHadInstability = false
        val sectionFloorY = anchorStore.anchoredFloorY(frame) ?: workingFloorY
        sectionFloorY?.let { workingFloorY = it }
        val snapshot = FloorFrameSnapshot(
            isTracking = isSessionTracking,
            horizontalPlaneCount = 1,
            selectedArea = workingFloorAreaM2,
            hasCenterHit = liveReticlePoint != null,
            isFloorDetected = isSessionTracking && liveReticlePoint != null,
            currentHitPoint = liveReticlePoint,
            focusedLabel = "placement",
            largestPlaneAreaM2 = workingFloorAreaM2
        )
        floorArController.onFrame(
            snapshot,
            anchorStore.placedPoints(
                frame = frame,
                sectionFloorY = sectionFloorY,
                trackingStable = isPlacementAnchorTrackingStable(frame),
                hadTrackingInstability = placementAnchorHadInstability
            )
        )
        clearPlacementAnchorInstabilityIfSettled(frame)
        publishContourRealignAvailability()
        syncContourRenderer()
    }

    fun dispatchEvent(event: FloorArEvent) {
        if (event == FloorArEvent.AddPoint) {
            handleAddPointTap()
            return
        }
        if (event == FloorArEvent.UndoPoint) {
            onPlacementHintChanged(null)
            val effects = floorArController.onEvent(event)
            applyEffects(effects)
            placementMutationCooldownFrames = PLACEMENT_MUTATION_COOLDOWN_FRAMES
            if (floorArController.currentState().placedPoints.isEmpty()) {
                workingFloorY = null
                workingFloorAreaM2 = 0f
                smoothedPlacementPatchPoint = null
                lastPlacementPatchSmoothSeconds = 0.0
                placementAnchorTrackingWasUnstable = false
        placementAnchorHadInstability = false
        placementAnchorRecoveryContextUntilSeconds = 0.0
                anchorStore.setSectionPlaneAnchorId(null)
                placementFreezeApplied = false
                planeSurfaceRenderer.exitPlacementHitOnlyMode()
                planeSurfaceRenderer.exitPlacementScanFreeze()
            }
            updateContourModeFromState()
            syncContourRenderer()
            publishContourRealignAvailability()
            return
        }
        val effects = floorArController.onEvent(event)
        applyEffects(effects)
        if (event == FloorArEvent.Reset) {
            onPlacementHintChanged(null)
            workingFloorY = null
            workingFloorAreaM2 = 0f
            smoothedPlacementPatchPoint = null
            lastPlacementPatchSmoothSeconds = 0.0
            placementAnchorTrackingWasUnstable = false
            placementAnchorHadInstability = false
            placementAnchorRecoveryContextUntilSeconds = 0.0
            anchorStore.setSectionPlaneAnchorId(null)
            placementFreezeApplied = false
            planeSurfaceRenderer.exitPlacementHitOnlyMode()
            planeSurfaceRenderer.exitPlacementScanFreeze()
        }
        updateContourModeFromState()
        syncContourRenderer()
        publishContourRealignAvailability()
    }

    private fun handleAddPointTap() {
        val state = floorArController.currentState()
        if (state.isPolygonClosed) {
            onPlacementHintChanged(null)
            floorArController.onEvent(FloorArEvent.FinalizeArea)
            updateContourModeFromState()
            syncContourRenderer()
            return
        }
        val view = sceneView
        if (view == null) {
            onPlacementHintChanged("AR-сессия не готова")
            return
        }
        val now = currentTimeSeconds()
        lastTapFrameAgeMs = computeHitAgeMs(now, liveReticleResolvedSeconds)
        val frame = view.session.currentFrame
        if (frame == null) {
            onPlacementHintChanged("Дождитесь попадания в плоскость под прицелом")
            return
        }
        val sectionFloorY = anchorStore.anchoredFloorY(frame)
            ?: workingFloorY
            ?: state.placedPoints.firstOrNull()?.position?.yMeters
        if (sectionFloorY != null) {
            workingFloorY = sectionFloorY
        }
        val freshHit = if (sectionFloorY != null) {
            view.resolveWorkingFloorPlacementHit(frame = frame, sectionFloorY = sectionFloorY)
        } else {
            view.resolveScanCenterHit(frame = frame, sectionFloorY = null)
        }.also {
            lastCenterHitResolvedSeconds = now
        }
        val liveReticleAgeMs = computeHitAgeMs(now, liveReticleResolvedSeconds)
        val renderFloorCandidate = if (sectionFloorY != null) {
            liveReticlePoint?.takeIf { liveReticleAgeMs <= LIVE_RETICLE_TAP_MAX_AGE_MS }
        } else {
            null
        }
        val freshRaw = freshHit.placementFloorPoint(sectionFloorY)
        val raw = freshRaw ?: renderFloorCandidate
        if (raw == null) {
            onPlacementHintChanged(
                if (sectionFloorY != null) {
                    "Повторите тап — позиция под прицелом обновилась"
                } else {
                    "Повторите тап — позиция под прицелом обновилась"
                }
            )
            return
        }
        val hit = if (freshRaw != null) freshHit else renderFloorCenterHit(raw)
        lastTapSourceLabel = if (freshRaw != null) "currentFrame" else "renderFloor"
        val projected = if (sectionFloorY == null) {
            val dominantFloorY = planeSurfaceRenderer.estimatedFloorWorldY() ?: raw.yMeters
            ArPoint3D(
                xMeters = raw.xMeters,
                yMeters = dominantFloorY,
                zMeters = raw.zMeters
            )
        } else {
            FloorGeometry.projectToSectionFloor(raw, sectionFloorY)
        }
        val previewAtTap = liveReticlePoint?.takeIf {
            liveReticleAgeMs <= LIVE_RETICLE_TAP_MAX_AGE_MS
        }
        lastTapDeltaCm = if (previewAtTap != null) {
            horizontalArPointDistanceMeters(previewAtTap, projected) * 100f
        } else {
            0f
        }
        if (previewAtTap != null && lastTapDeltaCm > TAP_MAX_PREVIEW_DELTA_CM) {
            onPlacementHintChanged("Повторите тап — позиция под прицелом обновилась")
            return
        }
        val tapState = FloorSnapReducer.applySnap(
            state.copy(
                currentHitPoint = projected,
                hasCenterHit = true
            )
        )
        when (val validation = FloorContourReducer.validateTapPlacement(tapState, projected)) {
            is AddPointValidation.Rejected -> onPlacementHintChanged(validation.reason.toIosHint())
            is AddPointValidation.Accepted -> {
                onPlacementHintChanged(null)
                val pointCountBefore = floorArController.currentState().placedPoints.size
                val initialReferenceAnchorId = if (pointCountBefore == 0) {
                    hit.confirmedSample?.anchor?.identifier ?: hit.anchor?.identifier
                } else {
                    null
                }
                if (pointCountBefore == 0) {
                    workingFloorY = validation.point.yMeters
                    smoothedPlacementPatchPoint = validation.point
                    placementPatchSmoother.reset(validation.point)
                    workingFloorAreaM2 = initialReferenceAnchorId?.let { planeSurfaceRenderer.area(it) }
                        ?: planeSurfaceRenderer.scanDebugStats().largestPlaneAreaM2
                    anchorStore.setSectionPlaneAnchorId(null)
                }
                val logicalId = NSUUID().UUIDString()
                anchorStore.register(view.session, logicalId, validation.point, frame)
                floorArController.onPointAdded(logicalId, validation.point)
                if (pointCountBefore == 0) {
                    planeSurfaceRenderer.lockSectionFloor(initialReferenceAnchorId, validation.point.yMeters)
                }
                placementMutationCooldownFrames = PLACEMENT_MUTATION_COOLDOWN_FRAMES
                syncContourRenderer()
                activatePlacementVisualizationImmediately(
                    view = view,
                    frame = frame,
                    projected = validation.point,
                    tapHit = hit
                )
            }
        }
    }

    @ObjCSignatureOverride
    override fun renderer(renderer: SCNSceneRendererProtocol, updateAtTime: Double) {
        updateWorkingFloorPatchFromRenderer(updateAtTime)
        syncFinalizedContourFromRenderer(updateAtTime)
    }

    @ObjCSignatureOverride
    override fun renderer(renderer: SCNSceneRendererProtocol, didAddNode: SCNNode, forAnchor: ARAnchor) {
        onPlaneAnchorNodeChanged(forAnchor, didAddNode, forceGeometry = true)
    }

    @ObjCSignatureOverride
    override fun renderer(renderer: SCNSceneRendererProtocol, didUpdateNode: SCNNode, forAnchor: ARAnchor) {
        onPlaneAnchorNodeChanged(forAnchor, didUpdateNode, forceGeometry = false)
    }

    @ObjCSignatureOverride
    override fun renderer(renderer: SCNSceneRendererProtocol, didRemoveNode: SCNNode, forAnchor: ARAnchor) {
        val planeAnchor = forAnchor as? ARPlaneAnchor ?: return
        val state = floorArController.currentState()
        if (state.placedPoints.isNotEmpty() && !state.isFinalized) return
        planeSurfaceRenderer.remove(planeAnchor.identifier)
    }

    override fun session(session: ARSession, didUpdateFrame: ARFrame) {
        val view = sceneView ?: return
        val frameStartedAt = currentTimeSeconds()
        trackDelegateWallGap(frameStartedAt)
        lastDelegateAtSeconds = frameStartedAt
        trackCameraFrameGap(didUpdateFrame.timestamp)
        updateSessionDelegateHz(frameStartedAt)

        val state = floorArController.currentState()
        val hasPlacedPoints = state.placedPoints.isNotEmpty()
        val placementScanFrozen = hasPlacedPoints && !state.isFinalized
        val sectionFloorY = workingFloorY ?: state.placedPoints.firstOrNull()?.position?.yMeters

        if (placementScanFrozen) {
            handlePlacementDidUpdateFrame(
                view = view,
                frame = didUpdateFrame,
                frameStartedAt = frameStartedAt,
                state = state,
                sectionFloorY = sectionFloorY
            )
            return
        }

        if (cameraFrameGapMs >= TRACKING_DEGRADED_CAMERA_GAP_MS) {
            planeSurfaceRenderer.requestOverlayTransformDebounce()
        }
        if (relocationController.isRelocalizing()) {
            planeSurfaceRenderer.requestOverlayTransformDebounce()
        }
        planeSurfaceRenderer.tickOverlayTransformDebounce()
        val trackingDegraded = isTrackingDegraded(cameraFrameGapMs, relocationController)
        planeSurfaceRenderer.applyReferenceOverlayDegraded(trackingDegraded)

        if (placementMutationCooldownFrames > 0) {
            placementMutationCooldownFrames--
        }
        val horizontalPlaneCount = didUpdateFrame.anchors.count {
            (it as? ARPlaneAnchor)?.isHorizontalTracking() == true
        }

        val runHitTest = state.showPlaneDots
        hitTestFrameCounter++
        val runFreshHitTest = runHitTest && hitTestFrameCounter % SCAN_HIT_TEST_INTERVAL_FRAMES == 0
        val centerHit = if (runFreshHitTest) {
            view.resolveScanCenterHit(
                frame = didUpdateFrame,
                sectionFloorY = sectionFloorY
            ).also {
                cachedCenterHit = it
                lastCenterHit = it
                lastCenterHitResolvedSeconds = frameStartedAt
            }
        } else {
            cachedCenterHit
        }

        val reticleAnchorId = centerHit.anchor?.identifier
        val focusedAnchorId = focusedPlaneTracker.update(reticleAnchorId)

        val scanContext = IosArScanSurfaceContext(
            allowEstimatedPatch = didUpdateFrame.isTrackingNormal(),
            strictConfirmedOverlaysOnly = didUpdateFrame.strictOutdoorScanMode(),
            cameraWorldYMeters = didUpdateFrame.cameraWorldYMeters(),
            cameraWorldPosition = didUpdateFrame.cameraWorldPosition()
        )
        planeSurfaceRenderer.exitPlacementHitOnlyMode()
        planeSurfaceRenderer.exitPlacementScanFreeze()
        maybePerformAutomaticScanReset(view.session, didUpdateFrame)
        syncScanSurfaceVisualization(
                view = view,
                frame = didUpdateFrame,
                centerHit = centerHit,
                scanContext = scanContext,
                reticleAnchorId = reticleAnchorId
            )

        val gridMode = planeSurfaceRenderer.activeMode()
        lastRendererMode = when {
            !state.showPlaneDots -> "contour-hidden"
            gridMode == GridSurfaceMode.MULTI_WITH_RETICLE -> "scan-multi+reticle"
            gridMode == GridSurfaceMode.MULTI_SURFACE -> "scan-multi-surface"
            gridMode == GridSurfaceMode.RETICLE_ONLY -> "scan-reticle-patch"
            else -> "scan-grid-hidden"
        }

        val selectedArea = if (centerHit.confirmed) {
            focusedAnchorId?.let { planeSurfaceRenderer.area(it) } ?: 0f
        } else {
            0f
        }
        val floorDetected = if (hasPlacedPoints) {
            isSessionTracking && (centerHit.confirmed || centerHit.previewWorldFloorPoint() != null)
        } else {
            isSessionTracking &&
                centerHit.confirmed &&
                selectedArea >= MIN_FLOOR_AREA_M2
        }
        val hasCenterHit = centerHit.confirmed
        val focusedLabel = buildFocusedLabel(
            gridMode = gridMode,
            focusedAnchorId = focusedAnchorId,
            overlayCount = planeSurfaceRenderer.overlayCount(),
            surfaceCount = planeSurfaceRenderer.visibleSurfaceCount(),
            inGracePeriod = focusedPlaneTracker.isInGracePeriod()
        )

        val currentHitPoint = centerHit.placementFloorPoint(sectionFloorY)?.let {
            FloorGeometry.projectToSectionFloor(it, sectionFloorY)
        }
        val hitAgeMs = computeHitAgeMs(frameStartedAt, lastCenterHitResolvedSeconds)
        val livePlacementStatus = placementStatusLabel(centerHit, sectionFloorY, hitAgeMs)
        val largestPlaneAreaM2 = planeSurfaceRenderer.scanDebugStats().largestPlaneAreaM2
        val snapshot = FloorFrameSnapshot(
            isTracking = isSessionTracking,
            horizontalPlaneCount = horizontalPlaneCount,
            selectedArea = selectedArea,
            hasCenterHit = hasCenterHit,
            isFloorDetected = floorDetected,
            currentHitPoint = currentHitPoint,
            focusedLabel = focusedLabel,
            largestPlaneAreaM2 = largestPlaneAreaM2
        )
        val trackingStable = isPlacementAnchorTrackingStable(didUpdateFrame)
        if (hasPlacedPoints) {
            if (!trackingStable) {
                markPlacementAnchorTrackingUnstable()
            } else {
                onPlacementAnchorTrackingRecovered(frameStartedAt)
            }
        }
        val updatedPoints = if (hasPlacedPoints) {
            anchorStore.placedPoints(
                frame = didUpdateFrame,
                sectionFloorY = sectionFloorY,
                trackingStable = trackingStable,
                hadTrackingInstability = placementAnchorHadInstability
            )
        } else {
            emptyList()
        }
        floorArController.onFrame(snapshot, updatedPoints)
        if (hasPlacedPoints) {
            clearPlacementAnchorInstabilityIfSettled(didUpdateFrame)
            publishContourRealignAvailability()
        }
        val snapActive = floorArController.currentState().snappedPointIndex != null
        if (!snapActive && canClearTapRejectionHint(livePlacementStatus, frameStartedAt, lastDelegateAtSeconds)) {
            onPlacementHintChanged(null)
        }
        syncContourRenderer()

        lastFrameHandleMs = ((currentTimeSeconds() - frameStartedAt) * 1000.0)
            .roundToInt()
            .coerceAtLeast(0)

        publishPlaneDebugMetrics(
            nowSeconds = frameStartedAt,
            centerHit = centerHit,
            scanContext = scanContext,
            largestPlaneAreaM2 = largestPlaneAreaM2,
            sectionFloorY = sectionFloorY,
            hitAgeMs = hitAgeMs,
            placementScanFrozen = false
        )
    }

    override fun session(session: ARSession, cameraDidChangeTrackingState: ARCamera) {
        val now = currentTimeSeconds()
        when (cameraDidChangeTrackingState.trackingState) {
            ARTrackingState.ARTrackingStateNotAvailable -> {
                isSessionTracking = false
                onTrackingNameChanged("NOT_AVAILABLE")
                markPlacementAnchorTrackingUnstable()
            }
            ARTrackingState.ARTrackingStateLimited -> {
                isSessionTracking = true
                onTrackingNameChanged("LIMITED")
                markPlacementAnchorTrackingUnstable()
            }
            ARTrackingState.ARTrackingStateNormal -> {
                isSessionTracking = true
                onTrackingNameChanged("TRACKING")
                onPlacementAnchorTrackingRecovered(now)
            }
            else -> {
                isSessionTracking = true
                onTrackingNameChanged("UNKNOWN")
                markPlacementAnchorTrackingUnstable()
            }
        }
        if (floorArController.currentState().placedPoints.isEmpty()) {
            relocationController.onTrackingStateChanged(cameraDidChangeTrackingState)?.let { request ->
                performScanReset(session, request)
            }
        }
    }

    override fun sessionWasInterrupted(session: ARSession) {
        relocationController.onSessionInterrupted()
        markPlacementAnchorTrackingUnstable()
        if (floorArController.currentState().placedPoints.isEmpty()) {
            planeSurfaceRenderer.requestOverlayTransformDebounce()
        }
    }

    override fun sessionInterruptionEnded(session: ARSession) {
        if (floorArController.currentState().placedPoints.isNotEmpty()) {
            onPlacementAnchorTrackingRecovered(currentTimeSeconds())
            return
        }
        if (!relocationController.onInterruptionEnded()) return
        performScanReset(session, RelocationResetRequest(reason = "interrupt-end"))
    }

    private fun onPlaneAnchorNodeChanged(
        forAnchor: ARAnchor,
        anchorNode: SCNNode,
        forceGeometry: Boolean
    ) {
        updateRendererNodeCallbackHz(currentTimeSeconds())
        val planeAnchor = forAnchor as? ARPlaneAnchor ?: return
        suppressArkitPlaneMesh(anchorNode)
        val state = floorArController.currentState()
        if (!state.showPlaneDots) return
        if (state.placedPoints.isNotEmpty() && !state.isFinalized) return
        if (!forceGeometry) {
            scanAnchorNodeUpdateCounter++
            if (scanAnchorNodeUpdateCounter % SCAN_ANCHOR_NODE_UPDATE_INTERVAL != 0) return
        }
        planeSurfaceRenderer.syncPlaneOverlayOnNodeEvent(
            anchor = planeAnchor,
            anchorNode = anchorNode,
            sceneRoot = sceneView?.scene?.rootNode,
            forceGeometry = forceGeometry
        )
    }

    private fun activatePlacementVisualizationImmediately(
        view: ARSCNView,
        frame: ARFrame,
        projected: ArPoint3D,
        tapHit: CenterPlaneHit
    ) {
        val sectionFloorY = workingFloorY ?: floorArController.currentState()
            .placedPoints
            .firstOrNull()
            ?.position
            ?.yMeters

        if (!placementFreezeApplied) {
            planeSurfaceRenderer.enterPlacementScanFreeze(view.scene?.rootNode)
            placementFreezeApplied = true
        }

        val now = currentTimeSeconds()
        updatePlacementReticle(view, frame, sectionFloorY, now)
        if (liveReticlePoint == null) {
            liveReticleHit = tapHit
            liveReticlePoint = projected
            liveReticleResolvedSeconds = now
            liveReticleSourceLabel = tapHit.hitPathDebugLabel()
        }

        syncPlacementWorkingFloorVisualization(view, liveReticleHit)

        val selectedArea = workingFloorAreaM2
        val placementStatus = placementStatusLabel(liveReticleHit, sectionFloorY, hitAgeMs = 0)
        val hasCenterHit = liveReticlePoint != null && placementStatus in PLACEABLE_STATUSES

        floorArController.onFrame(
            FloorFrameSnapshot(
                isTracking = isSessionTracking,
                horizontalPlaneCount = 1,
                selectedArea = selectedArea,
                hasCenterHit = hasCenterHit,
                isFloorDetected = isSessionTracking && hasCenterHit,
                currentHitPoint = liveReticlePoint ?: projected,
                focusedLabel = "placement",
                largestPlaneAreaM2 = selectedArea
            ),
            anchorStore.placedPoints(frame, sectionFloorY)
        )

        lastRendererMode = when {
            planeSurfaceRenderer.isPlacementExplorationPatchVisible() -> "working-floor+patch"
            planeSurfaceRenderer.visibleSurfaceCount() > 0 -> "continuous-floor"
            else -> "working-floor-waiting"
        }

        lastMetricsPublishSeconds = 0.0
        publishPlaneDebugMetrics(
            nowSeconds = now,
            centerHit = liveReticleHit,
            scanContext = IosArScanSurfaceContext(
                allowEstimatedPatch = frame.isTrackingNormal(),
                strictConfirmedOverlaysOnly = frame.strictOutdoorScanMode(),
                cameraWorldYMeters = frame.cameraWorldYMeters(),
                cameraWorldPosition = frame.cameraWorldPosition()
            ),
            largestPlaneAreaM2 = selectedArea,
            sectionFloorY = sectionFloorY,
            hitAgeMs = 0,
            placementScanFrozen = true
        )
    }

    private fun handlePlacementDidUpdateFrame(
        view: ARSCNView,
        frame: ARFrame,
        frameStartedAt: Double,
        state: FloorContourUiState,
        sectionFloorY: Float?
    ) {
        val trackingStable = isPlacementAnchorTrackingStable(frame)
        if (!trackingStable) {
            markPlacementAnchorTrackingUnstable()
        } else {
            onPlacementAnchorTrackingRecovered(frameStartedAt)
        }
        val effectiveSectionFloorY = anchorStore.anchoredFloorY(frame) ?: sectionFloorY
        if (effectiveSectionFloorY != null) {
            workingFloorY = effectiveSectionFloorY
        }
        if (!placementFreezeApplied) {
            planeSurfaceRenderer.enterPlacementScanFreeze(view.scene?.rootNode)
            placementFreezeApplied = true
        }
        if (state.showPlaneDots) {
            updatePlacementReticle(view, frame, effectiveSectionFloorY, frameStartedAt)
            syncPlacementExplorePatch(view, liveReticleHit)
        }
        val placementStatus = placementStatusLabel(
            liveReticleHit,
            effectiveSectionFloorY,
            hitAgeMs = computeHitAgeMs(frameStartedAt, liveReticleResolvedSeconds)
        )
        val hasCenterHit = liveReticlePoint != null && placementStatus in PLACEABLE_STATUSES
        val selectedArea = workingFloorAreaM2

        placementUiFrameCounter++
        val updatedPoints = anchorStore.placedPoints(
            frame = frame,
            sectionFloorY = effectiveSectionFloorY,
            trackingStable = trackingStable,
            hadTrackingInstability = placementAnchorHadInstability
        )
        val periodicSync = placementUiFrameCounter % PLACEMENT_ANCHORED_POINTS_SYNC_INTERVAL_FRAMES == 0
        val immediateSync = contourPointsMoved(state.placedPoints, updatedPoints)
        if (periodicSync || immediateSync) {
            val snapshot = FloorFrameSnapshot(
                isTracking = isSessionTracking,
                horizontalPlaneCount = 1,
                selectedArea = selectedArea,
                hasCenterHit = hasCenterHit,
                isFloorDetected = isSessionTracking && hasCenterHit,
                currentHitPoint = liveReticlePoint,
                focusedLabel = "placement",
                largestPlaneAreaM2 = selectedArea
            )
            floorArController.onFrame(snapshot, updatedPoints)
        }

        clearPlacementAnchorInstabilityIfSettled(frame)
        publishContourRealignAvailability()

        val snapActive = floorArController.currentState().snappedPointIndex != null
        if (!snapActive) {
            when {
                shouldShowPlacementCatchupHint(
                    isRelocalizing = relocationController.isRelocalizing(),
                    isTrackingNormal = frame.isTrackingNormal()
                ) -> onPlacementHintChanged("Трекинг догоняет…")
                canClearTapRejectionHint(placementStatus, frameStartedAt, lastDelegateAtSeconds) ->
                    onPlacementHintChanged(null)
            }
        }
        syncContourRenderer()

        lastFrameHandleMs = ((currentTimeSeconds() - frameStartedAt) * 1000.0)
            .roundToInt()
            .coerceAtLeast(0)

        lastRendererMode = when {
            planeSurfaceRenderer.isPlacementExplorationPatchVisible() -> "working-floor+patch"
            planeSurfaceRenderer.visibleSurfaceCount() > 0 -> "continuous-floor"
            else -> "working-floor-waiting"
        }

        publishPlaneDebugMetrics(
            nowSeconds = frameStartedAt,
            centerHit = liveReticleHit,
            scanContext = IosArScanSurfaceContext(
                allowEstimatedPatch = frame.isTrackingNormal(),
                strictConfirmedOverlaysOnly = frame.strictOutdoorScanMode(),
                cameraWorldYMeters = frame.cameraWorldYMeters(),
                cameraWorldPosition = frame.cameraWorldPosition()
            ),
            largestPlaneAreaM2 = selectedArea,
            sectionFloorY = effectiveSectionFloorY,
            hitAgeMs = computeHitAgeMs(frameStartedAt, liveReticleResolvedSeconds),
            placementScanFrozen = true
        )
    }

    private fun updateWorkingFloorPatchFromRenderer(renderTimeSeconds: Double) {
        val view = sceneView ?: return
        val state = floorArController.currentState()
        if (!state.showPlaneDots || state.placedPoints.isEmpty() || state.isFinalized) return

        val sectionFloorY = workingFloorY ?: state.placedPoints.firstOrNull()?.position?.yMeters ?: return
        workingFloorY = sectionFloorY
        if (renderTimeSeconds - lastPlacementRenderReticleSeconds < PLACEMENT_RENDER_RETICLE_MIN_INTERVAL_SECONDS) {
            return
        }
        lastPlacementRenderReticleSeconds = renderTimeSeconds

        val point = view.screenCenterFloorIntersection(sectionFloorY) ?: return
        val patchPoint = smoothPlacementPatchPoint(point, renderTimeSeconds)
        val now = currentTimeSeconds()
        val hit = renderFloorCenterHit(point)
        val visualHit = renderFloorCenterHit(patchPoint)
        liveReticleHit = hit
        liveReticlePoint = point
        liveReticleResolvedSeconds = now
        liveReticleSourceLabel = hit.hitPathDebugLabel()
        floorArController.updateLiveContourPoint(point)
        syncContourRenderer()

        val rootNode = view.scene?.rootNode ?: return
        if (!placementFreezeApplied) {
            planeSurfaceRenderer.enterPlacementScanFreeze(rootNode)
            placementFreezeApplied = true
        }
        if (planeSurfaceRenderer.isPlacementExplorationPatchVisible()) {
            planeSurfaceRenderer.updatePlacementExplorationPatchPosition(patchPoint, sectionFloorY)
        } else {
            planeSurfaceRenderer.syncPlacementExplorationPatch(
                rootNode = rootNode,
                centerHit = visualHit,
                sectionFloorY = sectionFloorY,
                show = true
            )
        }
        lastRendererMode = "working-floor+patch"
    }

    /** Keep fill/points/lines visible when delegate frames stall after close/finalize. */
    private fun syncFinalizedContourFromRenderer(renderTimeSeconds: Double) {
        val state = floorArController.currentState()
        val keepContourVisible = state.placedPoints.isNotEmpty() &&
            (state.isFinalized || state.isPolygonClosed)
        if (!keepContourVisible) return
        if (renderTimeSeconds - lastFinalizedContourRenderSyncSeconds <
            PLACEMENT_RENDER_RETICLE_MIN_INTERVAL_SECONDS
        ) {
            return
        }
        lastFinalizedContourRenderSyncSeconds = renderTimeSeconds
        syncContourRenderer()
    }

    private fun smoothPlacementPatchPoint(rawPoint: ArPoint3D, renderTimeSeconds: Double): ArPoint3D {
        val previousSmoothSeconds = lastPlacementPatchSmoothSeconds
        val dt = if (previousSmoothSeconds <= 0.0) {
            PLACEMENT_RENDER_RETICLE_MIN_INTERVAL_SECONDS
        } else {
            (renderTimeSeconds - previousSmoothSeconds)
                .coerceIn(PLACEMENT_RENDER_RETICLE_MIN_INTERVAL_SECONDS * 0.5, 0.05)
        }
        lastPlacementPatchSmoothSeconds = renderTimeSeconds

        val previous = smoothedPlacementPatchPoint
        if (previous == null) {
            placementPatchSmoother.reset(rawPoint)
            smoothedPlacementPatchPoint = rawPoint
            return rawPoint
        }

        val dx = rawPoint.xMeters - previous.xMeters
        val dz = rawPoint.zMeters - previous.zMeters
        val distanceSq = dx * dx + dz * dz
        val snapSq = PLACEMENT_RENDER_SMOOTHING_SNAP_M * PLACEMENT_RENDER_SMOOTHING_SNAP_M
        if (distanceSq >= snapSq) {
            placementPatchSmoother.reset(rawPoint)
            smoothedPlacementPatchPoint = rawPoint
            return rawPoint
        }

        val filtered = placementPatchSmoother.filter(rawPoint, dt)
        val next = blendPlacementPatchForFastMotion(
            filtered = filtered,
            raw = rawPoint,
            previous = previous,
            dt = dt
        )
        smoothedPlacementPatchPoint = next
        return next
    }

    private fun updatePlacementReticle(
        view: ARSCNView,
        frame: ARFrame,
        sectionFloorY: Float?,
        nowSeconds: Double
    ) {
        val hit = sectionFloorY?.let {
            view.resolveWorkingFloorPlacementHit(frame = frame, sectionFloorY = it)
        } ?: view.resolvePlacementLiveReticleHit(frame, sectionFloorY)
        val point = hit.placementFloorPoint(sectionFloorY)?.let {
            FloorGeometry.projectToSectionFloor(it, sectionFloorY)
        }
        liveReticleHit = hit
        liveReticlePoint = point
        liveReticleResolvedSeconds = nowSeconds
        liveReticleSourceLabel = hit.hitPathDebugLabel()
    }

    private fun syncPlacementExplorePatch(
        view: ARSCNView,
        centerHit: CenterPlaneHit
    ) {
        val state = floorArController.currentState()
        if (!state.showPlaneDots) return
        val rootNode = view.scene?.rootNode ?: return
        val sectionFloorY = workingFloorY ?: state.placedPoints.firstOrNull()?.position?.yMeters
        val placementPoint = smoothedPlacementPatchPoint ?: liveReticlePoint
        val placementStatus = placementStatusLabel(centerHit, sectionFloorY, hitAgeMs = 0)
        val showExplorePatch = shouldShowPlacementPatch(placementStatus) && placementPoint != null
        if (showExplorePatch) {
            if (!planeSurfaceRenderer.isPlacementExplorationPatchVisible()) {
                planeSurfaceRenderer.syncPlacementExplorationPatch(
                    rootNode = rootNode,
                    centerHit = centerHit,
                    sectionFloorY = sectionFloorY,
                    show = true
                )
            }
        } else {
            planeSurfaceRenderer.hidePlacementExplorationPatch()
        }
    }

    private fun publishScanResetUiSnapshot() {
        val now = currentTimeSeconds()
        lastArFrameTimestamp = null
        cameraFrameGapMs = 0
        delegateWallGapMs = 0
        delegateHzFrameCount = 0
        delegateHzWindowStartSeconds = now
        sessionDelegateHz = 0
        rendererNodeCallbackWindowStartSeconds = now
        rendererNodeCallbackCount = 0
        rendererNodeCallbackHz = 0
        lastRendererMode = "scan-reset"
        floorArController.onFrame(
            FloorFrameSnapshot(
                isTracking = isSessionTracking,
                horizontalPlaneCount = 0,
                selectedArea = 0f,
                hasCenterHit = false,
                isFloorDetected = false,
                currentHitPoint = null,
                focusedLabel = "scan-reset",
                largestPlaneAreaM2 = 0f
            ),
            emptyList()
        )
        lastMetricsPublishSeconds = 0.0
        publishPlaneDebugMetrics(
            nowSeconds = now,
            centerHit = CenterPlaneHit(),
            scanContext = IosArScanSurfaceContext(
                allowEstimatedPatch = true,
                strictConfirmedOverlaysOnly = false,
                cameraWorldYMeters = 0f,
                cameraWorldPosition = ArPoint3D(0f, 0f, 0f)
            ),
            largestPlaneAreaM2 = 0f,
            sectionFloorY = null,
            hitAgeMs = 0,
            placementScanFrozen = false
        )
    }

    private fun syncPlacementWorkingFloorVisualization(
        view: ARSCNView,
        centerHit: CenterPlaneHit
    ) {
        val state = floorArController.currentState()
        if (!state.showPlaneDots) {
            planeSurfaceRenderer.hideAll()
            return
        }
        val rootNode = view.scene?.rootNode ?: return
        val sectionFloorY = workingFloorY ?: state.placedPoints.firstOrNull()?.position?.yMeters
        val placementPoint = smoothedPlacementPatchPoint ?: liveReticlePoint
        val placementStatus = placementStatusLabel(centerHit, sectionFloorY, hitAgeMs = 0)
        val showExplorePatch = shouldShowPlacementPatch(placementStatus) && placementPoint != null
        if (showExplorePatch) {
            if (!planeSurfaceRenderer.isPlacementExplorationPatchVisible()) {
                val visualHit = renderFloorCenterHit(placementPoint!!)
                planeSurfaceRenderer.syncPlacementExplorationPatch(
                    rootNode = rootNode,
                    centerHit = visualHit,
                    sectionFloorY = sectionFloorY,
                    show = true
                )
            }
        } else {
            planeSurfaceRenderer.hidePlacementExplorationPatch()
        }
    }

    private fun syncScanSurfaceVisualization(
        view: ARSCNView,
        frame: ARFrame,
        centerHit: CenterPlaneHit,
        scanContext: IosArScanSurfaceContext,
        reticleAnchorId: NSUUID? = null
    ) {
        val state = floorArController.currentState()
        if (!state.showPlaneDots) {
            planeSurfaceRenderer.hideAll()
            return
        }

        scanOverlaySyncCounter++
        val horizontalPlanes = frame.anchors.filterIsInstance<ARPlaneAnchor>()
            .filter { it.isHorizontalTracking() }
        if (scanOverlaySyncCounter % SCAN_OVERLAY_BUDGET_INTERVAL_FRAMES == 0) {
            planeSurfaceRenderer.applyOverlayBudget(
                anchors = horizontalPlanes,
                scanContext = scanContext,
                reticleAnchorId = reticleAnchorId
            )
        }
        if (
            placementMutationCooldownFrames == 0 &&
            scanOverlaySyncCounter % SCAN_OVERLAY_ELEVATION_INTERVAL_FRAMES == 0
        ) {
            planeSurfaceRenderer.applyOverlayElevation(horizontalPlanes, centerHit)
        }

        val floorY = planeSurfaceRenderer.estimatedFloorWorldY()
        val largestPlaneAreaM2 = planeSurfaceRenderer.scanDebugStats().largestPlaneAreaM2
        val rootNode = view.scene?.rootNode
        val sectionFloorY = workingFloorY ?: state.placedPoints.firstOrNull()?.position?.yMeters
        val placementScanFrozen = state.placedPoints.isNotEmpty() && !state.isFinalized
        val nowSeconds = currentTimeSeconds()
        val renderedReticle = if (rootNode != null && placementScanFrozen) {
            val hitAgeMs = computeHitAgeMs(nowSeconds, lastCenterHitResolvedSeconds)
            val placementStatus = placementStatusLabel(centerHit, sectionFloorY, hitAgeMs)
            val showPatch = shouldShowPlacementPatch(placementStatus)
            planeSurfaceRenderer.syncPlacementExplorationPatch(
                rootNode = rootNode,
                centerHit = centerHit,
                sectionFloorY = sectionFloorY,
                show = showPatch
            )
            planeSurfaceRenderer.isPlacementExplorationPatchVisible()
        } else if (rootNode != null) {
            planeSurfaceRenderer.syncPlacementExplorationPatch(
                rootNode = rootNode,
                centerHit = centerHit,
                sectionFloorY = sectionFloorY,
                show = false
            )
            planeSurfaceRenderer.syncPlacementPatch(
                rootNode = rootNode,
                centerHit = centerHit,
                sectionFloorY = sectionFloorY,
                show = false
            )
            planeSurfaceRenderer.syncReticlePatch(
                rootNode,
                centerHit,
                floorY,
                scanContext,
                largestPlaneAreaM2
            )
        } else {
            false
        }

        val hasVisibleSurfaces = planeSurfaceRenderer.visibleSurfaceCount() > 0
        if ((hasVisibleSurfaces || renderedReticle) && firstSurfaceOverlayMs == null) {
            firstSurfaceOverlayMs = elapsedMsSinceSession(currentTimeSeconds())
        }
    }

    private fun applyEffects(effects: List<FloorArEffect>) {
        val session = sceneView?.session ?: return
        val frame = session.currentFrame
        effects.forEach { effect ->
            when (effect) {
                is FloorArEffect.CreateAnchorAt -> {
                    val logicalId = NSUUID().UUIDString()
                    anchorStore.register(session, logicalId, effect.point, frame)
                    floorArController.onPointAdded(logicalId, effect.point)
                }
                is FloorArEffect.DetachAnchor -> anchorStore.detachLast(session, frame)
                FloorArEffect.DetachAllAnchors -> anchorStore.detachAll(session, frame)
            }
        }
    }

    /** Hide scan overlays only after finalize (Android: planeRenderer until confirm). */
    private fun updateContourModeFromState() {
        val state = floorArController.currentState()
        if (!state.showPlaneDots) {
            planeSurfaceRenderer.enterContourMode()
            lastRendererMode = "contour-hidden"
        } else {
            planeSurfaceRenderer.exitContourMode()
        }
    }

    private fun syncContourRenderer() {
        contourRenderer.syncIfChanged(floorArController.currentState())
    }

    private fun isPlacementAnchorTrackingStable(frame: ARFrame): Boolean =
        frame.isTrackingNormal() &&
            !relocationController.isRelocalizing() &&
            cameraFrameGapMs < TRACKING_DEGRADED_CAMERA_GAP_MS

    private fun markPlacementAnchorTrackingUnstable() {
        if (floorArController.currentState().placedPoints.isEmpty()) return
        placementAnchorTrackingWasUnstable = true
        placementAnchorHadInstability = true
    }

    private fun clearPlacementAnchorInstabilityIfSettled(frame: ARFrame) {
        if (!placementAnchorHadInstability) return
        if (!isPlacementAnchorTrackingStable(frame)) return
        if (anchorStore.canManuallyRealign()) return
        when (anchorStore.correctionDebug().stateLabel) {
            "frozen-unstable", "pending-small" -> return
        }
        placementAnchorHadInstability = false
    }

    private fun contourPointsMoved(
        previous: List<PlacedContourPoint>,
        updated: List<PlacedContourPoint>,
        epsilonM: Float = 0.01f
    ): Boolean {
        if (previous.size != updated.size) return true
        return previous.zip(updated).any { (before, after) ->
            FloorGeometry.distancePlanar(before.position, after.position) > epsilonM
        }
    }

    private fun onPlacementAnchorTrackingRecovered(nowSeconds: Double) {
        if (floorArController.currentState().placedPoints.isEmpty()) return
        if (!placementAnchorTrackingWasUnstable) return
        placementAnchorTrackingWasUnstable = false
        val recoveryUntil = nowSeconds + PLACEMENT_ANCHOR_RECOVERY_CONTEXT_SECONDS
        if (recoveryUntil > placementAnchorRecoveryContextUntilSeconds) {
            placementAnchorRecoveryContextUntilSeconds = recoveryUntil
        }
    }

    private fun isPlacementAnchorRecoveryContextActive(nowSeconds: Double): Boolean =
        nowSeconds < placementAnchorRecoveryContextUntilSeconds

    private fun publishContourRealignAvailability() {
        onContourRealignAvailableChanged(anchorStore.canManuallyRealign())
    }

    private fun trackCameraFrameGap(arFrameTimestamp: Double) {
        lastArFrameTimestamp?.let { previousTimestamp ->
            cameraFrameGapMs = ((arFrameTimestamp - previousTimestamp) * 1000.0)
                .roundToInt()
                .coerceIn(1, 9_999)
        }
        lastArFrameTimestamp = arFrameTimestamp
    }

    private fun trackDelegateWallGap(nowSeconds: Double) {
        if (lastDelegateAtSeconds > 0.0) {
            delegateWallGapMs = ((nowSeconds - lastDelegateAtSeconds) * 1000.0)
                .roundToInt()
                .coerceIn(1, 9_999)
        }
    }

    /** How often [session:didUpdateFrame:] runs — not SceneKit or plane-renderer FPS. */
    private fun updateSessionDelegateHz(nowSeconds: Double) {
        delegateHzFrameCount++
        val elapsed = nowSeconds - delegateHzWindowStartSeconds
        if (elapsed >= 1.0) {
            sessionDelegateHz = (delegateHzFrameCount / elapsed).roundToInt()
            delegateHzFrameCount = 0
            delegateHzWindowStartSeconds = nowSeconds
        }
    }

    private fun updateRendererNodeCallbackHz(nowSeconds: Double) {
        rendererNodeCallbackCount++
        val elapsed = nowSeconds - rendererNodeCallbackWindowStartSeconds
        if (elapsed >= 1.0) {
            rendererNodeCallbackHz = (rendererNodeCallbackCount / elapsed).roundToInt()
            rendererNodeCallbackCount = 0
            rendererNodeCallbackWindowStartSeconds = nowSeconds
        }
    }

    private fun maybePerformAutomaticScanReset(session: ARSession, frame: ARFrame) {
        relocationController.distantPlaneResetRequest(frame)?.let { request ->
            performScanReset(session, request)
        }
    }

    private fun performScanReset(
        session: ARSession,
        request: RelocationResetRequest,
        force: Boolean = false
    ) {
        if (!force && floorArController.currentState().placedPoints.isNotEmpty()) return
        performScanSessionReset(
            session = session,
            request = request,
            relocationController = relocationController,
            force = force,
            callbacks = ScanSessionResetCallbacks(
                onRendererReset = {
                    placementMutationCooldownFrames = 0
                    planeSurfaceRenderer.reset()
                    planeSurfaceRenderer.prepare()
                },
                onFocusReset = { focusedPlaneTracker.reset() },
                onHitCacheReset = {
                    cachedCenterHit = CenterPlaneHit()
                    lastCenterHit = CenterPlaneHit()
                    lastCenterHitResolvedSeconds = 0.0
                    clearLiveReticleHit()
                    lastDelegateAtSeconds = 0.0
                    delegateWallGapMs = 0
                    lastTapFrameAgeMs = 0
                    lastTapDeltaCm = 0f
                    lastTapSourceLabel = "-"
                    placementFreezeApplied = false
                    workingFloorY = null
                    workingFloorAreaM2 = 0f
                    placementUiFrameCounter = 0
                    lastPlacementRenderReticleSeconds = 0.0
                    lastPlacementPatchSmoothSeconds = 0.0
                    scanOverlaySyncCounter = 0
                    scanAnchorNodeUpdateCounter = 0
                    rendererNodeCallbackWindowStartSeconds = currentTimeSeconds()
                    rendererNodeCallbackCount = 0
                    rendererNodeCallbackHz = 0
                    hitTestFrameCounter = 0
                },
                onOverlayLatencyReset = { firstSurfaceOverlayMs = null }
            )
        )
    }

    private fun clearLiveReticleHit() {
        liveReticleHit = CenterPlaneHit()
        liveReticlePoint = null
        smoothedPlacementPatchPoint = null
        lastPlacementPatchSmoothSeconds = 0.0
        liveReticleResolvedSeconds = 0.0
        liveReticleSourceLabel = "none"
        planeSurfaceRenderer.hidePlacementExplorationPatch()
    }

    private fun publishPlaneDebugMetrics(
        nowSeconds: Double,
        centerHit: CenterPlaneHit,
        scanContext: IosArScanSurfaceContext,
        largestPlaneAreaM2: Float,
        sectionFloorY: Float?,
        hitAgeMs: Int,
        placementScanFrozen: Boolean
    ) {
        if (nowSeconds - lastMetricsPublishSeconds < METRICS_PUBLISH_INTERVAL_SECONDS) return
        lastMetricsPublishSeconds = nowSeconds
        val patchLabel = when {
            planeSurfaceRenderer.isPlacementFallbackPatchVisible() -> "fallback-on"
            planeSurfaceRenderer.isPlacementExplorationPatchVisible() -> "explore-on"
            !scanContext.allowEstimatedPatch -> "off-limited"
            planeSurfaceRenderer.isReticlePatchVisible() &&
                !centerHit.confirmed &&
                largestPlaneAreaM2 >= MIN_FLOOR_AREA_M2 -> "search-on"
            planeSurfaceRenderer.isReticlePatchVisible() -> "on"
            centerHit.previewSample != null || centerHit.previewHitResult != null -> "est-blocked"
            else -> "off"
        }
        val scanStats = planeSurfaceRenderer.scanDebugStats()
        val hitY = centerHit.confirmedWorldFloorPoint()?.yMeters
            ?: centerHit.previewWorldFloorPoint()?.yMeters
        val deltaY = if (hitY != null && sectionFloorY != null) hitY - sectionFloorY else null
        val placementStatus = placementStatusLabel(centerHit, sectionFloorY, hitAgeMs)
        val reticleHitAgeMs = if (placementScanFrozen) {
            computeHitAgeMs(nowSeconds, liveReticleResolvedSeconds)
        } else {
            0
        }
        val liveTapFrameAgeMs = lastTapFrameAgeMs
        val hitYLabel = when {
            hitY == null -> "-"
            sectionFloorY == null -> "raw:${hitY.formatMeters3()}"
            deltaY != null -> "raw:${hitY.formatMeters3()} sec:${sectionFloorY.formatMeters3()} d:${deltaY.formatMeters3()}"
            else -> "-"
        }
        onPlaneDebugMetricsChanged(
            IosPlaneDebugMetrics(
                sessionPhase = currentSessionPhaseLabel(),
                perfDiagnosis = diagnoseArPerf(
                    delegateHz = sessionDelegateHz,
                    cameraGapMs = cameraFrameGapMs,
                    frameHandleMs = lastFrameHandleMs
                ),
                sessionDelegateHz = sessionDelegateHz,
                cameraFrameGapMs = cameraFrameGapMs,
                cameraFrameGapLabel = formatCameraFrameGapLabel(cameraFrameGapMs),
                delegateWallGapMs = delegateWallGapMs,
                delegateWallGapLabel = if (delegateWallGapMs <= 0) "n/a" else "$delegateWallGapMs ms",
                frameHandleMs = lastFrameHandleMs,
                rendererNodeCallbackHz = rendererNodeCallbackHz,
                rendererMode = lastRendererMode,
                overlayCount = planeSurfaceRenderer.overlayCount(),
                anchorLatencyMs = firstSurfaceOverlayMs,
                sessionFeatures = sessionFeatures.debugLabel(),
                hitPath = centerHit.hitPathDebugLabel(),
                scanPatch = patchLabel,
                detectGate = if (centerHit.confirmed) "confirmed" else "searching",
                placementStatus = placementStatus,
                hitAgeMs = hitAgeMs,
                reticleHitAgeMs = reticleHitAgeMs,
                reticleHitAgeLabel = formatReticleHitAgeLabel(reticleHitAgeMs),
                reticleSourceLabel = if (placementScanFrozen) liveReticleSourceLabel else "delegate",
                tapFrameAgeMs = liveTapFrameAgeMs,
                tapFrameAgeLabel = formatTapFrameAgeLabel(liveTapFrameAgeMs, liveReticleResolvedSeconds),
                tapSourceLabel = lastTapSourceLabel,
                tapDeltaCm = lastTapDeltaCm,
                tapDeltaLabel = formatTapDeltaLabel(lastTapDeltaCm),
                trackingQualityLabel = formatTrackingQualityLabel(
                    trackingDegraded = isTrackingDegraded(cameraFrameGapMs, relocationController),
                    overlayDebouncing = planeSurfaceRenderer.isOverlayTransformDebouncing()
                ),
                hitYLabel = hitYLabel,
                largestPlaneAreaM2 = scanStats.largestPlaneAreaM2,
                relocLabel = relocationController.relocLabel(),
                cullLabel = scanStats.cullStats.debugLabel(),
                anchorCorrectionLabel = formatAnchorCorrectionLabel(
                    stateLabel = anchorStore.correctionDebug().stateLabel,
                    rootDeltaCm = anchorStore.correctionDebug().rootDeltaCm,
                    displayDeltaCm = anchorStore.correctionDebug().displayDeltaCm
                ),
                anchorRootDeltaCm = anchorStore.correctionDebug().rootDeltaCm,
                anchorDisplayDeltaCm = anchorStore.correctionDebug().displayDeltaCm
            )
        )
    }

    private fun currentSessionPhaseLabel(): String {
        val state = floorArController.currentState()
        return when {
            !state.showPlaneDots -> "contour"
            state.placedPoints.isNotEmpty() && !state.isFinalized -> "placement"
            else -> "scan"
        }
    }

    private fun elapsedMsSinceSession(nowSeconds: Double): Int =
        ((nowSeconds - sessionStartTimeSeconds) * 1000.0).roundToInt()

    private fun buildFocusedLabel(
        gridMode: GridSurfaceMode,
        focusedAnchorId: NSUUID?,
        overlayCount: Int,
        surfaceCount: Int,
        inGracePeriod: Boolean
    ): String {
        if (gridMode == GridSurfaceMode.MULTI_WITH_RETICLE) {
            return "multi+patch/$overlayCount"
        }
        if (gridMode == GridSurfaceMode.MULTI_SURFACE) {
            val suffix = if (inGracePeriod) " (hold)" else ""
            return "multi/$surfaceCount$suffix"
        }
        if (gridMode == GridSurfaceMode.RETICLE_ONLY) {
            return "patch/$overlayCount"
        }
        if (focusedAnchorId == null) return "No"
        val suffix = if (inGracePeriod) " (hold)" else ""
        return "surface/$overlayCount$suffix"
    }
}
