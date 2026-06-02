package com.example.arplitka.iosapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import kotlinx.cinterop.useContents
import platform.ARKit.*
import platform.CoreGraphics.CGPointMake
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

    val floorArController = remember {
        FloorArController(
            onStateChanged = { contourState = it }
        )
    }

    val coordinator = remember {
        IosArSessionCoordinator(
            floorArController = floorArController,
            onStateChanged = { contourState = it },
            onTrackingNameChanged = { trackingStateName = it }
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
            instructionText = contourState.instruction.toIosText(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 36.dp)
        )

        CenterReticle(
            modifier = Modifier.align(Alignment.Center),
            isActive = contourState.hasCenterHit
        )

        if (!contourState.isFinalized) {
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
                    "Area" to "${(contourState.selectedArea * 100).roundToInt() / 100.0} m²",
                    "Tracking" to trackingStateName,
                    "Center hit" to if (contourState.hasCenterHit) "Yes" else "No",
                    "Points" to contourState.placedPoints.size.toString(),
                    "Closed" to if (contourState.isPolygonClosed) "Yes" else "No"
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
    }
}

@OptIn(ExperimentalForeignApi::class)
private class IosArSessionCoordinator(
    private val floorArController: FloorArController,
    private val onStateChanged: (FloorContourUiState) -> Unit,
    private val onTrackingNameChanged: (String) -> Unit
) : NSObject(), ARSCNViewDelegateProtocol, ARSessionDelegateProtocol {

    private var sceneView: ARSCNView? = null
    private var isSessionTracking = false
    private var lastPlaneCount: Int = -1
    private var lastConfirmedCenterHit: Boolean = false
    private var lastFloorDetected: Boolean = false
    private var lastSelectedArea: Float = -1f
    private var lastFocusedLabel: String = ""
    private var lastConfirmedHitResult: ARHitTestResult? = null
    private var floorDotMaterial = createFloorDotMaterial()
    private val planeFingerprints: PlaneFingerprints = mutableMapOf()
    private val planeAreas: PlaneAreas = mutableMapOf()
    private val planeDotCounts: PlaneDotCounts = mutableMapOf()
    private val focusedPlaneTracker = FocusedPlaneTracker()
    private val anchorStore = IosFloorAnchorStore()
    private val contourRenderer = IosArContourRenderer()
    private var lastCenterLocal: Pair<Float, Float>? = null
    private var lastRenderedFocusedAnchorId: NSUUID? = null
    private var dotGridSyncProgress: DotGridSyncProgress? = null

    fun attach(sceneView: ARSCNView) {
        this.sceneView = sceneView
        planeFingerprints.clear()
        planeAreas.clear()
        planeDotCounts.clear()
        focusedPlaneTracker.reset()
        lastCenterLocal = null
        lastRenderedFocusedAnchorId = null
        dotGridSyncProgress = null
        lastPlaneCount = -1
        lastConfirmedCenterHit = false
        lastFloorDetected = false
        lastSelectedArea = -1f
        lastFocusedLabel = ""
        lastConfirmedHitResult = null
        isSessionTracking = false
        floorDotMaterial = createFloorDotMaterial()
        sceneView.debugOptions = 0UL
        sceneView.delegate = this
        sceneView.session.delegate = this
        sceneView.scene?.rootNode?.let { contourRenderer.attach(it) }
        sceneView.session.runWithConfiguration(
            createConfiguration(),
            ARSessionRunOptionResetTracking or ARSessionRunOptionRemoveExistingAnchors
        )
    }

    fun pause() {
        sceneView?.session?.let { session ->
            anchorStore.detachAll(session, session.currentFrame)
        }
        contourRenderer.detach()
        sceneView?.session?.pause()
        sceneView?.delegate = null
        sceneView?.session?.delegate = null
        planeFingerprints.clear()
        planeAreas.clear()
        planeDotCounts.clear()
        focusedPlaneTracker.reset()
        lastRenderedFocusedAnchorId = null
        dotGridSyncProgress = null
        sceneView = null
    }

    fun dispatchEvent(event: FloorArEvent) {
        val effects = floorArController.onEvent(event)
        applyEffects(effects)
        syncContourRenderer()
        onStateChanged(floorArController.currentState())
    }

    @ObjCSignatureOverride
    override fun renderer(renderer: SCNSceneRendererProtocol, didAddNode: SCNNode, forAnchor: ARAnchor) {
        // Dots are rendered from ARPlaneAnchor geometry in didUpdateFrame.
    }

    @ObjCSignatureOverride
    override fun renderer(renderer: SCNSceneRendererProtocol, didUpdateNode: SCNNode, forAnchor: ARAnchor) {
        // See didAddNode.
    }

    @ObjCSignatureOverride
    override fun renderer(renderer: SCNSceneRendererProtocol, didRemoveNode: SCNNode, forAnchor: ARAnchor) {
        val planeAnchor = forAnchor as? ARPlaneAnchor ?: return
        planeFingerprints.remove(planeAnchor.identifier)
        planeAreas.remove(planeAnchor.identifier)
        planeDotCounts.remove(planeAnchor.identifier)
        removePlaneDotGrid(didRemoveNode)
    }

    override fun session(session: ARSession, didUpdateFrame: ARFrame) {
        val view = sceneView ?: return

        dotGridSyncProgress?.let { progress ->
            dotGridSyncProgress = continueDotGridSync(progress)
        }

        val horizontalPlanes = didUpdateFrame.anchors
            .filterIsInstance<ARPlaneAnchor>()
            .filter { it.isHorizontalTracking() }

        val centerHit = view.centerPlaneHit()
        lastConfirmedHitResult = centerHit.confirmedHitResult
        val focusedAnchorId = focusedPlaneTracker.update(centerHit.anchor?.identifier)
        updatePlaneDotVisualization(view, didUpdateFrame, focusedAnchorId, centerHit)
        val hasVisualCenterHit = centerHit.confirmed || centerHit.previewHitResult != null

        val selectedArea = focusedAnchorId?.let { planeAreas[it] } ?: 0f
        val floorDetected = focusedAnchorId != null && selectedArea >= MIN_FLOOR_AREA_M2
        val focusedLabel = buildFocusedLabel(
            focusedAnchorId = focusedAnchorId,
            dotCount = focusedAnchorId?.let { planeDotCounts[it] } ?: 0,
            inGracePeriod = focusedPlaneTracker.isInGracePeriod()
        )

        val snapshot = FloorFrameSnapshot(
            isTracking = isSessionTracking,
            horizontalPlaneCount = horizontalPlanes.size,
            selectedArea = selectedArea,
            hasCenterHit = hasVisualCenterHit,
            isFloorDetected = floorDetected,
            currentHitPoint = centerHit.confirmedHitResult?.let { HitTransformReader.worldFloorPoint(it) },
            focusedLabel = focusedLabel
        )
        val updatedPoints = anchorStore.readPositions(didUpdateFrame)
        floorArController.onFrame(snapshot, updatedPoints)
        syncContourRenderer()

        if (
            lastPlaneCount != horizontalPlanes.size ||
            lastConfirmedCenterHit != hasVisualCenterHit ||
            lastFloorDetected != floorDetected ||
            lastSelectedArea != selectedArea ||
            lastFocusedLabel != focusedLabel
        ) {
            lastPlaneCount = horizontalPlanes.size
            lastConfirmedCenterHit = hasVisualCenterHit
            lastFloorDetected = floorDetected
            lastSelectedArea = selectedArea
            lastFocusedLabel = focusedLabel
            onStateChanged(floorArController.currentState())
        }
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
    }

    private fun applyEffects(effects: List<FloorArEffect>) {
        val session = sceneView?.session ?: return
        val frame = session.currentFrame
        effects.forEach { effect ->
            when (effect) {
                is FloorArEffect.CreateAnchorAt -> {
                    val hit = lastConfirmedHitResult ?: return@forEach
                    val logicalId = NSUUID().UUIDString()
                    val anchor = ARAnchor(transform = hit.worldTransform)
                    session.addAnchor(anchor)
                    anchorStore.register(logicalId, anchor)
                    floorArController.onPointAdded(
                        logicalId,
                        HitTransformReader.worldFloorPoint(hit)
                    )
                }
                is FloorArEffect.DetachAnchor -> anchorStore.detachLast(session, frame)
                FloorArEffect.DetachAllAnchors -> anchorStore.detachAll(session, frame)
            }
        }
    }

    private fun syncContourRenderer() {
        contourRenderer.sync(floorArController.currentState())
    }

    private fun updatePlaneDotVisualization(
        view: ARSCNView,
        frame: ARFrame,
        focusedAnchorId: NSUUID?,
        centerHit: CenterPlaneHit
    ) {
        if (centerHit.localPoint != null) {
            lastCenterLocal = centerHit.localPoint
        }
        val ref = centerHit.localPoint ?: lastCenterLocal
        val rootNode = view.scene?.rootNode

        if (focusedAnchorId != lastRenderedFocusedAnchorId) {
            lastRenderedFocusedAnchorId?.let { previousId ->
                frame.anchors
                    .filterIsInstance<ARPlaneAnchor>()
                    .firstOrNull { it.identifier == previousId }
                    ?.let { view.nodeForAnchor(it) }
                    ?.let { hidePlaneDotGrid(it) }
            }
            dotGridSyncProgress = null
            lastRenderedFocusedAnchorId = focusedAnchorId
        }

        if (focusedAnchorId == null) {
            centerHit.previewHitResult?.let { previewHit ->
                rootNode?.let {
                    syncPreviewDotGrid(
                        rootNode = it,
                        hitResult = previewHit,
                        dotMaterial = floorDotMaterial
                    )
                }
            } ?: rootNode?.let { hidePreviewDotGrid(it) }
            return
        }
        if (dotGridSyncProgress != null) return
        rootNode?.let { hidePreviewDotGrid(it) }

        val focusedAnchor = frame.anchors
            .filterIsInstance<ARPlaneAnchor>()
            .firstOrNull { it.identifier == focusedAnchorId }
            ?: return
        val anchorNode = view.nodeForAnchor(focusedAnchor) ?: return

        val viewFingerprint = focusedAnchor.computeViewFingerprint(ref, VISIBLE_DOT_RADIUS_M)
        val previousFingerprint = planeFingerprints[focusedAnchor.identifier]
        if (viewFingerprint == previousFingerprint) return

        val geometry = focusedAnchor.readPlaneGeometry(
            centerLocal = ref,
            visibleRadiusM = VISIBLE_DOT_RADIUS_M
        ) ?: run {
            hidePlaneDotGrid(anchorNode)
            return
        }

        dotGridSyncProgress = syncPlaneGeometryDots(
            parentNode = anchorNode,
            geometry = geometry,
            dotMaterial = floorDotMaterial,
            previousFingerprint = previousFingerprint,
            inProgress = dotGridSyncProgress
        )
        planeFingerprints[focusedAnchor.identifier] = geometry.fingerprint
        planeAreas[focusedAnchor.identifier] = geometry.area
        planeDotCounts[focusedAnchor.identifier] = geometry.dots.size
    }

    private fun buildFocusedLabel(
        focusedAnchorId: NSUUID?,
        dotCount: Int,
        inGracePeriod: Boolean
    ): String {
        if (focusedAnchorId == null) return "No"
        val suffix = if (inGracePeriod) " (hold)" else ""
        return "$dotCount pts$suffix"
    }

    private fun ARSCNView.centerPlaneHit(): CenterPlaneHit {
        val width = bounds.useContents { size.width }
        val height = bounds.useContents { size.height }
        if (width <= 0.0 || height <= 0.0) return CenterPlaneHit()

        val center = CGPointMake(width / 2.0, height / 2.0)
        val hitResult = hitTest(center, ARHitTestResultTypeExistingPlaneUsingExtent)
            .firstHorizontalFloorHitResult()
        val anchor = hitResult?.anchor as? ARPlaneAnchor
        val localPoint = hitResult?.let { HitTransformReader.localFloorPoint(it) }
        val confirmed = anchor != null &&
            localPoint != null &&
            anchor.containsLocalPoint(localPoint.first, localPoint.second)

        return CenterPlaneHit(
            confirmed = confirmed,
            anchor = if (confirmed) anchor else null,
            localPoint = if (confirmed) localPoint else null,
            confirmedHitResult = if (confirmed) hitResult else null,
            previewHitResult = if (confirmed) {
                null
            } else {
                hitTest(center, ARHitTestResultTypeEstimatedHorizontalPlane)
                    .firstEstimatedHorizontalFloorHitResult()
            }
        )
    }

    private fun createConfiguration(): ARWorldTrackingConfiguration =
        ARWorldTrackingConfiguration().apply {
            planeDetection = ARPlaneDetectionHorizontal
        }
}

private data class CenterPlaneHit(
    val confirmed: Boolean = false,
    val anchor: ARPlaneAnchor? = null,
    val localPoint: Pair<Float, Float>? = null,
    val confirmedHitResult: ARHitTestResult? = null,
    val previewHitResult: ARHitTestResult? = null
)

private fun ArTrackingStatus.toIosText(): String = when (this) {
    ArTrackingStatus.INITIALIZING -> "Инициализация…"
    ArTrackingStatus.SEARCHING_FLOOR -> "Наведите прицел на поверхность"
    ArTrackingStatus.FLOOR_DETECTED -> "Поверхность под прицелом"
    ArTrackingStatus.TRACKING_LOST -> "Трекинг потерян"
    ArTrackingStatus.POLYGON_CLOSED -> "Контур замкнут"
    ArTrackingStatus.FINALIZED -> ""
}

private fun ArInstruction.toIosText(): String = when (this) {
    ArInstruction.PLEASE_WAIT -> "Пожалуйста, подождите"
    ArInstruction.SEARCHING -> "Наведите прицел на нужную поверхность"
    ArInstruction.MOVE_PHONE -> "Медленно наведите камеру на поверхность"
    ArInstruction.DETECTED -> "Точки показывают поверхность под прицелом"
    ArInstruction.CONTOUR_CLOSED -> "Нажмите OK, чтобы завершить разметку"
    ArInstruction.CONTOUR_CONFIRMED -> "Нажмите «Добавить плитку» для предпросмотра"
    ArInstruction.TILE_VISIBLE -> "Плитка наложена. Можно повернуть или сменить"
    ArInstruction.EMPTY -> ""
}
