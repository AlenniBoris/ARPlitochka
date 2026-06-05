package com.example.arplitka.iosapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import com.example.arplitka.shared.ar.contracts.model.ArInstruction
import com.example.arplitka.shared.ar.contracts.model.ArTrackingStatus
import com.example.arplitka.shared.ar.contracts.state.FloorArEvent
import com.example.arplitka.shared.ar.domain.FloorArController
import com.example.arplitka.shared.ar.domain.FloorArEffect
import com.example.arplitka.shared.ar.domain.logic.AddPointRejectReason
import com.example.arplitka.shared.ar.domain.logic.AddPointValidation
import com.example.arplitka.shared.ar.domain.logic.FloorContourReducer
import com.example.arplitka.shared.ar.domain.logic.FloorGeometry
import com.example.arplitka.shared.ar.domain.model.FloorContourUiState
import com.example.arplitka.shared.ar.domain.model.FloorFrameSnapshot
import com.example.arplitka.shared.ui.kit.ArContourActionButtons
import com.example.arplitka.shared.ui.kit.ArTopBar
import com.example.arplitka.shared.ui.kit.CenterReticle
import com.example.arplitka.shared.ui.kit.DebugPanel
import com.example.arplitka.shared.ui.kit.StatusPanel
import com.example.arplitka.shared.ui.kit.isDebugBuild
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import platform.ARKit.*
import platform.CoreFoundation.CFAbsoluteTimeGetCurrent
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSUUID
import platform.SceneKit.SCNNode
import platform.SceneKit.SCNSceneRendererProtocol
import platform.darwin.NSObject
import kotlin.math.roundToInt

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun IosArScreen(onBack: () -> Unit) {
    var contourState by remember { mutableStateOf(FloorContourUiState()) }
    var trackingStateName by remember { mutableStateOf("INITIALIZING") }
    var planeDebugMetrics by remember { mutableStateOf(IosPlaneDebugMetrics()) }
    var placementHint by remember { mutableStateOf<String?>(null) }

    val floorArController = remember {
        FloorArController(
            onStateChanged = { contourState = it }
        )
    }

    val coordinator = remember {
        IosArSessionCoordinator(
            floorArController = floorArController,
            onTrackingNameChanged = { trackingStateName = it },
            onPlaneDebugMetricsChanged = { planeDebugMetrics = it },
            onPlacementHintChanged = { placementHint = it }
        )
    }

    DisposableEffect(coordinator) {
        onDispose {
            coordinator.pause()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        UIKitView(
            factory = {
                val sceneView = ARSCNView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0))
                coordinator.attach(sceneView)
                sceneView
            },
            modifier = Modifier.fillMaxSize()
        )

        StatusPanel(
            statusText = contourState.trackingStatus.toIosText(),
            instructionText = placementHint ?: contourState.instruction.toIosText(),
            detailText = contourState.toStatusDetailText(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 36.dp)
        )

        CenterReticle(
            modifier = Modifier.align(Alignment.Center),
            isActive = contourState.hasCenterHit && contourState.showContourActions
        )

        if (contourState.showContourActions) {
            ArContourActionButtons(
                hasCenterHit = contourState.hasCenterHit,
                isPolygonClosed = contourState.isPolygonClosed,
                hasPoints = contourState.placedPoints.isNotEmpty(),
                onAddPoint = { coordinator.dispatchEvent(FloorArEvent.AddPoint) },
                onUndoPoint = { coordinator.dispatchEvent(FloorArEvent.UndoPoint) },
                addContentDescription = "Добавить точку",
                undoContentDescription = "Отменить",
                okContentDescription = "Готово",
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        if (isDebugBuild()) {
            DebugPanel(
                debugLines = mapOf(
                    "Planes" to contourState.horizontalPlaneCount.toString(),
                    "Focused" to contourState.focusedLabel,
                    "Reticle area" to "${(contourState.selectedArea * 100).roundToInt() / 100.0} m²",
                    "Tracking" to trackingStateName,
                    "Center hit" to if (contourState.hasCenterHit) "Yes" else "No",
                    "Points" to contourState.placedPoints.size.toString(),
                    "Closed" to if (contourState.isPolygonClosed) "Yes" else "No",
                    "Plane renderer" to planeDebugMetrics.rendererMode,
                    "Plane FPS" to planeDebugMetrics.fps.toString(),
                    "Surface overlays" to planeDebugMetrics.overlayCount.toString(),
                    "AR features" to planeDebugMetrics.sessionFeatures,
                    "Hit path" to planeDebugMetrics.hitPath,
                    "Detect gate" to planeDebugMetrics.detectGate,
                    "Scan patch" to planeDebugMetrics.scanPatch,
                    "Largest plane" to "${(planeDebugMetrics.largestPlaneAreaM2 * 100).roundToInt() / 100.0} m²",
                    "Reloc" to planeDebugMetrics.relocLabel,
                    "Cull" to planeDebugMetrics.cullLabel
                ),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .padding(bottom = 100.dp)
            )
        }

        ArTopBar(
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopStart)
        )

        if (contourState.placedPoints.isEmpty()) {
            TextButton(
                onClick = { coordinator.rescanSession() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 36.dp, end = 12.dp)
            ) {
                Text(
                    text = "Пересканировать",
                    color = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class IosArSessionCoordinator(
    private val floorArController: FloorArController,
    private val onTrackingNameChanged: (String) -> Unit,
    private val onPlaneDebugMetricsChanged: (IosPlaneDebugMetrics) -> Unit,
    private val onPlacementHintChanged: (String?) -> Unit
) : NSObject(), ARSCNViewDelegateProtocol, ARSessionDelegateProtocol {

    private var sceneView: ARSCNView? = null
    private var isSessionTracking = false
    private var sessionFeatures: Set<IosArSessionFeature> = emptySet()
    private val focusedPlaneTracker = FocusedPlaneTracker()
    private val anchorStore = IosFloorAnchorStore()
    private val contourRenderer = IosArContourRenderer()
    private val planeSurfaceRenderer = IosArPlaneSurfaceRenderer()
    private val relocationController = IosArSessionRelocationController()
    private var pendingPlacementHit: CenterPlaneHit? = null
    private var cachedCenterHit: CenterPlaneHit = CenterPlaneHit()
    private var lastCenterHit: CenterPlaneHit = CenterPlaneHit()
    private var hitTestFrameCounter: Int = 0
    private var sessionStartTimeSeconds: Double = 0.0
    private var fpsWindowStartSeconds: Double = 0.0
    private var fpsFrameCount: Int = 0
    private var currentFps: Int = 0
    private var lastMetricsPublishSeconds: Double = 0.0
    private var lastRendererMode: String = "none"
    private var firstSurfaceOverlayMs: Int? = null

    fun attach(sceneView: ARSCNView) {
        val now = currentTimeSeconds()
        this.sceneView = sceneView
        focusedPlaneTracker.reset()
        relocationController.reset()
        planeSurfaceRenderer.reset()
        planeSurfaceRenderer.prepare()
        hitTestFrameCounter = 0
        cachedCenterHit = CenterPlaneHit()
        lastCenterHit = CenterPlaneHit()
        isSessionTracking = false
        sessionStartTimeSeconds = now
        fpsWindowStartSeconds = now
        fpsFrameCount = 0
        currentFps = 0
        lastMetricsPublishSeconds = 0.0
        lastRendererMode = "none"
        firstSurfaceOverlayMs = null
        sceneView.debugOptions = 0UL
        sceneView.delegate = this
        sceneView.session.delegate = this
        sceneView.scene?.rootNode?.let { contourRenderer.attach(it) }
        val (configuration, features) = createWorldTrackingConfiguration(enableLidarMesh = true)
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
            onPlacementHintChanged("Очистите контур перед пересканированием")
            return
        }
        onPlacementHintChanged(null)
        performScanReset(
            session = view.session,
            request = RelocationResetRequest(reason = "manual"),
            force = true
        )
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
            updateContourModeFromState()
            syncContourRendererIfNeeded()
            return
        }
        val effects = floorArController.onEvent(event)
        applyEffects(effects)
        updateContourModeFromState()
        syncContourRendererIfNeeded()
    }

    private fun handleAddPointTap() {
        val state = floorArController.currentState()
        if (state.isPolygonClosed) {
            onPlacementHintChanged(null)
            floorArController.onEvent(FloorArEvent.FinalizeArea)
            syncContourRendererIfNeeded()
            return
        }
        val view = sceneView
        if (view == null) {
            onPlacementHintChanged("AR-сессия не готова")
            return
        }
        val frame = view.session.currentFrame
        val hit = view.resolveCenterPlaneHit(frame = frame, session = view.session)
        lastCenterHit = hit
        val raw = hit.confirmedWorldFloorPoint()
        if (raw == null) {
            onPlacementHintChanged("Дождитесь точного попадания в плоскость под прицелом")
            return
        }
        val sectionFloorY = state.placedPoints.firstOrNull()?.position?.yMeters
        val projected = FloorGeometry.projectToSectionFloor(raw, sectionFloorY)
        when (val validation = FloorContourReducer.validateAddPoint(state, projected)) {
            is AddPointValidation.Rejected -> onPlacementHintChanged(validation.reason.toIosHint())
            is AddPointValidation.Accepted -> {
                onPlacementHintChanged(null)
                pendingPlacementHit = hit
                val wasFirstPoint = state.placedPoints.isEmpty()
                val effects = floorArController.onEvent(FloorArEvent.AddPointAt(validation.point))
                applyEffects(effects)
                pendingPlacementHit = null
                if (wasFirstPoint) {
                    planeSurfaceRenderer.enterContourMode()
                    lastRendererMode = "contour-hidden"
                }
                syncContourRendererIfNeeded()
            }
        }
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
        planeSurfaceRenderer.remove(planeAnchor.identifier)
    }

    override fun session(session: ARSession, didUpdateFrame: ARFrame) {
        val view = sceneView ?: return
        val frameStartedAt = currentTimeSeconds()
        updateFps(frameStartedAt)

        val state = floorArController.currentState()
        val contourActive = state.placedPoints.isNotEmpty()
        val horizontalPlaneCount = didUpdateFrame.anchors.count {
            (it as? ARPlaneAnchor)?.isHorizontalTracking() == true
        }

        hitTestFrameCounter++
        val runHitTest = !contourActive || hitTestFrameCounter % 3 == 0
        val centerHit = if (runHitTest) {
            view.resolveCenterPlaneHit(frame = didUpdateFrame, session = view.session).also {
                cachedCenterHit = it
                lastCenterHit = it
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
        if (!contourActive) {
            maybePerformAutomaticScanReset(view.session, didUpdateFrame)
        }
        syncScanSurfaceVisualization(view, didUpdateFrame, centerHit, scanContext)

        val gridMode = planeSurfaceRenderer.activeMode()
        lastRendererMode = when {
            !state.showPlaneDots -> "hidden"
            contourActive -> "contour-hidden"
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
        val floorDetected = isSessionTracking &&
            centerHit.confirmed &&
            selectedArea >= MIN_FLOOR_AREA_M2
        val hasCenterHit = centerHit.confirmed
        val focusedLabel = buildFocusedLabel(
            gridMode = gridMode,
            focusedAnchorId = focusedAnchorId,
            overlayCount = planeSurfaceRenderer.overlayCount(),
            surfaceCount = planeSurfaceRenderer.visibleSurfaceCount(),
            inGracePeriod = focusedPlaneTracker.isInGracePeriod()
        )

        val sectionFloorY = state.placedPoints.firstOrNull()?.position?.yMeters
        val currentHitPoint = centerHit.confirmedWorldFloorPoint()?.let {
            FloorGeometry.projectToSectionFloor(it, sectionFloorY)
        }
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
        val updatedPoints = anchorStore.placedPoints(sectionFloorY)
        floorArController.onFrame(snapshot, updatedPoints)

        publishPlaneDebugMetrics(frameStartedAt, centerHit, scanContext, largestPlaneAreaM2)
    }

    override fun session(session: ARSession, cameraDidChangeTrackingState: ARCamera) {
        when (cameraDidChangeTrackingState.trackingState) {
            ARTrackingState.ARTrackingStateNotAvailable -> {
                isSessionTracking = false
                onTrackingNameChanged("NOT_AVAILABLE")
            }
            ARTrackingState.ARTrackingStateLimited -> {
                isSessionTracking = true
                onTrackingNameChanged("LIMITED")
            }
            ARTrackingState.ARTrackingStateNormal -> {
                isSessionTracking = true
                onTrackingNameChanged("TRACKING")
            }
            else -> {
                isSessionTracking = true
                onTrackingNameChanged("UNKNOWN")
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
    }

    override fun sessionInterruptionEnded(session: ARSession) {
        if (floorArController.currentState().placedPoints.isNotEmpty()) return
        if (!relocationController.onInterruptionEnded()) return
        performScanReset(session, RelocationResetRequest(reason = "interrupt-end"))
    }

    private fun onPlaneAnchorNodeChanged(
        forAnchor: ARAnchor,
        anchorNode: SCNNode,
        forceGeometry: Boolean
    ) {
        val planeAnchor = forAnchor as? ARPlaneAnchor ?: return
        val state = floorArController.currentState()
        if (!state.showPlaneDots || state.placedPoints.isNotEmpty()) {
            planeSurfaceRenderer.cacheArea(planeAnchor)
            return
        }
        planeSurfaceRenderer.syncPlaneOverlayOnNodeEvent(
            anchor = planeAnchor,
            anchorNode = anchorNode,
            forceGeometry = forceGeometry
        )
    }

    private fun syncScanSurfaceVisualization(
        view: ARSCNView,
        frame: ARFrame,
        centerHit: CenterPlaneHit,
        scanContext: IosArScanSurfaceContext
    ) {
        val state = floorArController.currentState()
        if (!state.showPlaneDots || state.placedPoints.isNotEmpty()) {
            planeSurfaceRenderer.hideAll()
            return
        }

        val horizontalPlanes = frame.anchors.filterIsInstance<ARPlaneAnchor>()
            .filter { it.isHorizontalTracking() }
        planeSurfaceRenderer.applyOverlayBudget(horizontalPlanes, scanContext)
        planeSurfaceRenderer.applyOverlayElevation(horizontalPlanes, centerHit)

        val floorY = planeSurfaceRenderer.estimatedFloorWorldY()
        val largestPlaneAreaM2 = planeSurfaceRenderer.scanDebugStats().largestPlaneAreaM2
        val rootNode = view.scene?.rootNode
        val renderedReticle = if (rootNode != null) {
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
                    val transform = pendingPlacementHit?.placementWorldTransform()
                        ?: lastCenterHit.placementWorldTransform()
                        ?: return@forEach
                    val logicalId = NSUUID().UUIDString()
                    val anchor = ARAnchor(transform = transform)
                    session.addAnchor(anchor)
                    anchorStore.register(logicalId, anchor, effect.point)
                    floorArController.onPointAdded(logicalId, effect.point)
                }
                is FloorArEffect.DetachAnchor -> anchorStore.detachLast(session, frame)
                FloorArEffect.DetachAllAnchors -> anchorStore.detachAll(session, frame)
            }
        }
    }

    private fun updateContourModeFromState() {
        val hasPoints = floorArController.currentState().placedPoints.isNotEmpty()
        if (hasPoints) {
            planeSurfaceRenderer.enterContourMode()
            lastRendererMode = "contour-hidden"
        } else {
            planeSurfaceRenderer.exitContourMode()
            lastRendererMode = "scan-multi-surface"
        }
    }

    private fun syncContourRendererIfNeeded() {
        contourRenderer.syncIfChanged(floorArController.currentState())
    }

    private fun updateFps(nowSeconds: Double) {
        fpsFrameCount++
        val elapsed = nowSeconds - fpsWindowStartSeconds
        if (elapsed >= 1.0) {
            currentFps = (fpsFrameCount / elapsed).roundToInt()
            fpsFrameCount = 0
            fpsWindowStartSeconds = nowSeconds
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
        performScanSessionReset(
            session = session,
            request = request,
            relocationController = relocationController,
            force = force,
            callbacks = ScanSessionResetCallbacks(
                onRendererReset = {
                    planeSurfaceRenderer.reset()
                    planeSurfaceRenderer.prepare()
                },
                onFocusReset = { focusedPlaneTracker.reset() },
                onHitCacheReset = {
                    cachedCenterHit = CenterPlaneHit()
                    lastCenterHit = CenterPlaneHit()
                    hitTestFrameCounter = 0
                },
                onOverlayLatencyReset = { firstSurfaceOverlayMs = null }
            )
        )
    }

    private fun publishPlaneDebugMetrics(
        nowSeconds: Double,
        centerHit: CenterPlaneHit,
        scanContext: IosArScanSurfaceContext,
        largestPlaneAreaM2: Float
    ) {
        if (nowSeconds - lastMetricsPublishSeconds < METRICS_PUBLISH_INTERVAL_SECONDS) return
        lastMetricsPublishSeconds = nowSeconds
        val patchLabel = when {
            !scanContext.allowEstimatedPatch -> "off-limited"
            planeSurfaceRenderer.isReticlePatchVisible() &&
                !centerHit.confirmed &&
                largestPlaneAreaM2 >= MIN_FLOOR_AREA_M2 -> "search-on"
            planeSurfaceRenderer.isReticlePatchVisible() -> "on"
            centerHit.previewSample != null || centerHit.previewHitResult != null -> "est-blocked"
            else -> "off"
        }
        val scanStats = planeSurfaceRenderer.scanDebugStats()
        onPlaneDebugMetricsChanged(
            IosPlaneDebugMetrics(
                fps = currentFps,
                rendererMode = lastRendererMode,
                overlayCount = planeSurfaceRenderer.overlayCount(),
                anchorLatencyMs = firstSurfaceOverlayMs,
                sessionFeatures = sessionFeatures.debugLabel(),
                hitPath = centerHit.hitPathDebugLabel(),
                scanPatch = patchLabel,
                detectGate = if (centerHit.confirmed) "confirmed" else "searching",
                largestPlaneAreaM2 = scanStats.largestPlaneAreaM2,
                relocLabel = relocationController.relocLabel(),
                cullLabel = scanStats.cullStats.debugLabel()
            )
        )
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

private data class IosPlaneDebugMetrics(
    val fps: Int = 0,
    val rendererMode: String = "none",
    val overlayCount: Int = 0,
    val anchorLatencyMs: Int? = null,
    val sessionFeatures: String = "planes",
    val hitPath: String = "none",
    val detectGate: String = "searching",
    val scanPatch: String = "off",
    val largestPlaneAreaM2: Float = 0f,
    val relocLabel: String = "ok",
    val cullLabel: String = "d:0/e:0"
)

private const val METRICS_PUBLISH_INTERVAL_SECONDS = 0.25

private fun currentTimeSeconds(): Double =
    CFAbsoluteTimeGetCurrent()

private fun ArTrackingStatus.toIosText(): String = when (this) {
    ArTrackingStatus.INITIALIZING -> "Инициализация…"
    ArTrackingStatus.SEARCHING_FLOOR -> "Наведите прицел на поверхность"
    ArTrackingStatus.FLOOR_DETECTED -> "Поверхность под прицелом"
    ArTrackingStatus.TRACKING_LOST -> "Трекинг потерян"
    ArTrackingStatus.POLYGON_CLOSED -> "Контур замкнут"
    ArTrackingStatus.FINALIZED -> ""
}

private fun AddPointRejectReason.toIosHint(): String = when (this) {
    AddPointRejectReason.FINALIZED -> "Разметка уже завершена"
    AddPointRejectReason.POLYGON_CLOSED -> "Сначала подтвердите контур"
    AddPointRejectReason.SNAP_ACTIVE -> "Отведите прицел от точки"
    AddPointRejectReason.NO_HIT -> "Наведите прицел на поверхность"
    AddPointRejectReason.TOO_CLOSE_TO_LAST -> "Отойдите дальше от предыдущей точки"
    AddPointRejectReason.HEIGHT_OUT_OF_RANGE -> "Точка слишком высоко или низко относительно контура"
}

private fun FloorContourUiState.toStatusDetailText(): String? {
    val formatArea = { value: Float ->
        "${(value * 100).roundToInt() / 100.0} m²"
    }
    return when {
        isFloorDetected && selectedArea > 0f ->
            "Под прицелом: ${formatArea(selectedArea)}"
        largestPlaneAreaM2 >= MIN_FLOOR_AREA_M2 ->
            "Крупнейшая поверхность: ${formatArea(largestPlaneAreaM2)}"
        else -> null
    }
}

private fun ArInstruction.toIosText(): String = when (this) {
    ArInstruction.PLEASE_WAIT -> "Пожалуйста, подождите"
    ArInstruction.SEARCHING -> "Наведите прицел на нужную поверхность"
    ArInstruction.SURFACE_NEARBY -> "Сетка найдена — наведите прицел на неё"
    ArInstruction.MOVE_PHONE -> "Медленно наведите камеру на поверхность"
    ArInstruction.DETECTED -> "Точки показывают поверхность под прицелом"
    ArInstruction.CONTOUR_CLOSED -> "Нажмите OK, чтобы завершить разметку"
    ArInstruction.CONTOUR_CONFIRMED -> "Нажмите «Добавить плитку» для предпросмотра"
    ArInstruction.TILE_VISIBLE -> "Плитка наложена. Можно повернуть или сменить"
    ArInstruction.EMPTY -> ""
}
