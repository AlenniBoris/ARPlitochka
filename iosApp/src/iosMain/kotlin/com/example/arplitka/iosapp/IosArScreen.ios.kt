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
import platform.CoreFoundation.CFAbsoluteTimeGetCurrent
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
    var planeDebugMetrics by remember { mutableStateOf(IosPlaneDebugMetrics()) }

    val floorArController = remember {
        FloorArController(
            onStateChanged = { contourState = it }
        )
    }

    val coordinator = remember {
        IosArSessionCoordinator(
            floorArController = floorArController,
            onTrackingNameChanged = { trackingStateName = it },
            onPlaneDebugMetricsChanged = { planeDebugMetrics = it }
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
            isActive = contourState.hasCenterHit && contourState.showPlaneDots
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
                    "Area" to "${(contourState.selectedArea * 100).roundToInt() / 100.0} m²",
                    "Tracking" to trackingStateName,
                    "Center hit" to if (contourState.hasCenterHit) "Yes" else "No",
                    "Points" to contourState.placedPoints.size.toString(),
                    "Closed" to if (contourState.isPolygonClosed) "Yes" else "No",
                    "Plane renderer" to planeDebugMetrics.rendererMode,
                    "Plane FPS" to planeDebugMetrics.fps.toString(),
                    "Plane dots" to "${planeDebugMetrics.dotCount}/${planeDebugMetrics.nodeCount}",
                    "Scan buckets" to planeDebugMetrics.bucketCount.toString(),
                    "Dot gen/sync" to "${planeDebugMetrics.generateMs} / ${planeDebugMetrics.syncMs} ms",
                    "Dot latency" to "${planeDebugMetrics.previewLatencyMs} / ${planeDebugMetrics.anchorLatencyMs} ms",
                    "AR features" to planeDebugMetrics.sessionFeatures,
                    "Hit path" to planeDebugMetrics.hitPath
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
    private val onTrackingNameChanged: (String) -> Unit,
    private val onPlaneDebugMetricsChanged: (IosPlaneDebugMetrics) -> Unit
) : NSObject(), ARSCNViewDelegateProtocol, ARSessionDelegateProtocol {

    private var sceneView: ARSCNView? = null
    private var isSessionTracking = false
    private var lastPlaneCount: Int = -1
    private var lastConfirmedCenterHit: Boolean = false
    private var lastFloorDetected: Boolean = false
    private var lastSelectedArea: Float = -1f
    private var lastFocusedLabel: String = ""
    private var lastCenterHit: CenterPlaneHit = CenterPlaneHit()
    private var sessionFeatures: Set<IosArSessionFeature> = emptySet()
    private var floorDotMaterial = createFloorDotMaterial()
    private val planeFingerprints: PlaneFingerprints = mutableMapOf()
    private val planeAreas: PlaneAreas = mutableMapOf()
    private val planeDotCounts: PlaneDotCounts = mutableMapOf()
    private val polygonStableFrames: PlanePolygonStableFrames = mutableMapOf()
    private val dotBucketAccumulator = PlaneDotBucketAccumulator()
    private val planeDotElevationLock = PlaneDotElevationLock()
    private val focusedPlaneTracker = FocusedPlaneTracker()
    private val anchorStore = IosFloorAnchorStore()
    private val contourRenderer = IosArContourRenderer()
    private var lastCenterLocal: Pair<Float, Float>? = null
    private var lastRenderedFocusedAnchorId: NSUUID? = null
    private var sessionStartTimeSeconds: Double = 0.0
    private var fpsWindowStartSeconds: Double = 0.0
    private var fpsFrameCount: Int = 0
    private var currentFps: Int = 0
    private var lastMetricsPublishSeconds: Double = 0.0
    private var firstPreviewDotsMs: Int? = null
    private var firstAnchorDotsMs: Int? = null
    private var lastPlaneFrameStats = PlaneDotFrameStats()
    private var lastMeshRebuildSeconds: Double = 0.0
    private var lastMeshBucketFingerprint: UInt? = null

    fun attach(sceneView: ARSCNView) {
        val now = currentTimeSeconds()
        this.sceneView = sceneView
        planeFingerprints.clear()
        planeAreas.clear()
        planeDotCounts.clear()
        polygonStableFrames.clear()
        dotBucketAccumulator.clearAll()
        planeDotElevationLock.clearAll()
        focusedPlaneTracker.reset()
        lastMeshRebuildSeconds = 0.0
        lastMeshBucketFingerprint = null
        lastCenterLocal = null
        lastRenderedFocusedAnchorId = null
        lastPlaneCount = -1
        lastConfirmedCenterHit = false
        lastFloorDetected = false
        lastSelectedArea = -1f
        lastFocusedLabel = ""
        lastCenterHit = CenterPlaneHit()
        isSessionTracking = false
        sessionStartTimeSeconds = now
        fpsWindowStartSeconds = now
        fpsFrameCount = 0
        currentFps = 0
        lastMetricsPublishSeconds = 0.0
        firstPreviewDotsMs = null
        firstAnchorDotsMs = null
        lastPlaneFrameStats = PlaneDotFrameStats()
        floorDotMaterial = createFloorDotMaterial()
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
        sceneView?.session?.pause()
        sceneView?.delegate = null
        sceneView?.session?.delegate = null
        planeFingerprints.clear()
        planeAreas.clear()
        planeDotCounts.clear()
        polygonStableFrames.clear()
        dotBucketAccumulator.clearAll()
        planeDotElevationLock.clearAll()
        focusedPlaneTracker.reset()
        lastMeshRebuildSeconds = 0.0
        lastMeshBucketFingerprint = null
        lastRenderedFocusedAnchorId = null
        sceneView = null
    }

    fun dispatchEvent(event: FloorArEvent) {
        val effects = floorArController.onEvent(event)
        applyEffects(effects)
        syncContourRenderer()
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
        polygonStableFrames.remove(planeAnchor.identifier)
        dotBucketAccumulator.remove(planeAnchor.identifier)
        planeDotElevationLock.clear(planeAnchor.identifier)
        removePlaneDotGrid(didRemoveNode)
    }

    override fun session(session: ARSession, didUpdateFrame: ARFrame) {
        val view = sceneView ?: return
        val frameStartedAt = currentTimeSeconds()
        updateFps(frameStartedAt)

        val horizontalPlanes = didUpdateFrame.anchors
            .filterIsInstance<ARPlaneAnchor>()
            .filter { it.isHorizontalTracking() }

        val centerHit = view.resolveCenterPlaneHit()
        lastCenterHit = centerHit
        val focusedAnchorId = focusedPlaneTracker.update(centerHit.anchor?.identifier)
        lastPlaneFrameStats = if (floorArController.currentState().showPlaneDots) {
            updatePlaneDotVisualization(view, didUpdateFrame, focusedAnchorId, centerHit, frameStartedAt)
        } else {
            hidePlaneDotVisualization(view, didUpdateFrame)
            PlaneDotFrameStats(rendererMode = "hidden")
        }
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
            currentHitPoint = centerHit.confirmedWorldFloorPoint()
                ?: centerHit.previewWorldFloorPoint(),
            focusedLabel = focusedLabel
        )
        val updatedPoints = anchorStore.readPositions(didUpdateFrame)
        floorArController.onFrame(snapshot, updatedPoints)
        syncContourRenderer()

        lastPlaneCount = horizontalPlanes.size
        lastConfirmedCenterHit = hasVisualCenterHit
        lastFloorDetected = floorDetected
        lastSelectedArea = selectedArea
        lastFocusedLabel = focusedLabel
        publishPlaneDebugMetrics(frameStartedAt, centerHit)
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
                    val transform = lastCenterHit.placementWorldTransform() ?: return@forEach
                    val logicalId = NSUUID().UUIDString()
                    val anchor = ARAnchor(transform = transform)
                    session.addAnchor(anchor)
                    anchorStore.register(logicalId, anchor)
                    floorArController.onPointAdded(
                        logicalId,
                        lastCenterHit.confirmedWorldFloorPoint() ?: effect.point
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
        centerHit: CenterPlaneHit,
        frameStartedAtSeconds: Double
    ): PlaneDotFrameStats {
        if (centerHit.localPoint != null) {
            lastCenterLocal = centerHit.localPoint
        }
        val ref = centerHit.localPoint ?: lastCenterLocal
        val rootNode = view.scene?.rootNode

        if (focusedAnchorId != lastRenderedFocusedAnchorId) {
            lastRenderedFocusedAnchorId?.let { previousId ->
                planeDotElevationLock.clear(previousId)
                frame.anchors
                    .filterIsInstance<ARPlaneAnchor>()
                    .firstOrNull { it.identifier == previousId }
                    ?.let { view.nodeForAnchor(it) }
                    ?.let { hidePlaneDotGrid(it) }
            }
            lastRenderedFocusedAnchorId = focusedAnchorId
            lastMeshRebuildSeconds = 0.0
            lastMeshBucketFingerprint = null
        }

        if (focusedAnchorId == null) {
            if (centerHit.previewHitResult != null) {
                val syncStart = currentTimeSeconds()
                val previewDotCount = rootNode?.let {
                    syncPreviewDotGrid(
                        rootNode = it,
                        centerHit = centerHit,
                        dotMaterial = floorDotMaterial
                    )
                } ?: 0
                firstPreviewDotsMs = firstPreviewDotsMs ?: elapsedMsSinceSession(frameStartedAtSeconds)
                return PlaneDotFrameStats(
                    rendererMode = "preview-hit",
                    dotCount = previewDotCount,
                    nodeCount = rootNode?.let { activePreviewDotNodeCount(it) } ?: 0,
                    syncMs = elapsedMs(syncStart)
                )
            }
            rootNode?.let { hidePreviewDotGrid(it) }
            return PlaneDotFrameStats(rendererMode = "none")
        }
        rootNode?.let { hidePreviewDotGrid(it) }

        val focusedAnchor = frame.anchors
            .filterIsInstance<ARPlaneAnchor>()
            .firstOrNull { it.identifier == focusedAnchorId }
            ?: return PlaneDotFrameStats(rendererMode = "anchor-missing")
        val anchorNode = view.nodeForAnchor(focusedAnchor)
            ?: return PlaneDotFrameStats(rendererMode = "anchor-node-missing")

        updatePolygonStableFrames(focusedAnchor)

        val boundaryMode = selectAccumulateBoundaryMode(
            anchor = focusedAnchor,
            polygonStableFrames = polygonStableFrames[focusedAnchor.identifier] ?: 0
        )
        val refForMesh = ref ?: (0f to 0f)
        if (ref != null) {
            dotBucketAccumulator.recordScan(
                anchorId = focusedAnchor.identifier,
                localX = ref.first,
                localZ = ref.second
            )
        }

        val displayFingerprint = combinedDisplayFingerprint(
            anchor = focusedAnchor,
            accumulator = dotBucketAccumulator,
            boundaryMode = boundaryMode,
            centerLocal = ref,
            visibleRadiusM = VISIBLE_DOT_RADIUS_M
        )
        val previousFingerprint = planeFingerprints[focusedAnchor.identifier]
        val storedBucketCount = dotBucketAccumulator.bucketCount(focusedAnchor.identifier)
        if (displayFingerprint == previousFingerprint) {
            val activeNodes = activePlaneDotNodeCount(anchorNode)
            return finishPlaneDotFrame(
                anchorNode,
                focusedAnchor,
                centerHit,
                lastPlaneFrameStats.copy(
                    rendererMode = "accumulate-cached",
                    dotCount = planeDotCounts[focusedAnchor.identifier] ?: activeNodes,
                    nodeCount = activeNodes,
                    bucketCount = storedBucketCount
                )
            )
        }

        val nowSeconds = currentTimeSeconds()
        if (
            nowSeconds - lastMeshRebuildSeconds < MESH_REBUILD_MIN_INTERVAL_SECONDS &&
            displayFingerprint == lastMeshBucketFingerprint
        ) {
            return finishPlaneDotFrame(
                anchorNode,
                focusedAnchor,
                centerHit,
                lastPlaneFrameStats.copy(
                    rendererMode = "accumulate-throttled",
                    bucketCount = storedBucketCount
                )
            )
        }

        val generateStart = currentTimeSeconds()
        val geometry = buildCombinedPlaneGeometry(
            anchor = focusedAnchor,
            accumulator = dotBucketAccumulator,
            boundaryMode = boundaryMode,
            centerLocal = ref,
            visibleRadiusM = VISIBLE_DOT_RADIUS_M
        ) ?: buildHitDiscGeometry(refForMesh)
        val generateMs = elapsedMs(generateStart)

        if (geometry == null) {
            hidePlaneDotGrid(anchorNode)
            return finishPlaneDotFrame(
                anchorNode,
                focusedAnchor,
                centerHit,
                PlaneDotFrameStats(
                    rendererMode = "accumulate-empty",
                    bucketCount = storedBucketCount
                )
            )
        }

        val syncStart = currentTimeSeconds()
        syncPlaneGeometryDots(
            parentNode = anchorNode,
            geometry = geometry,
            dotMaterial = floorDotMaterial,
            previousFingerprint = previousFingerprint
        )
        val syncMs = elapsedMs(syncStart)
        planeFingerprints[focusedAnchor.identifier] = geometry.fingerprint
        planeAreas[focusedAnchor.identifier] = geometry.area
        planeDotCounts[focusedAnchor.identifier] = geometry.dotCount
        lastMeshRebuildSeconds = nowSeconds
        lastMeshBucketFingerprint = displayFingerprint
        if (geometry.dotCount > 0) {
            firstAnchorDotsMs = firstAnchorDotsMs ?: elapsedMsSinceSession(frameStartedAtSeconds)
        }
        return finishPlaneDotFrame(
            anchorNode,
            focusedAnchor,
            centerHit,
            PlaneDotFrameStats(
                rendererMode = "${PlaneDotMeshSource.ACCUMULATE.debugLabel}+live+${boundaryMode.debugLabel}",
                dotCount = geometry.dotCount,
                nodeCount = activePlaneDotNodeCount(anchorNode),
                generateMs = generateMs,
                syncMs = syncMs,
                bucketCount = storedBucketCount
            )
        )
    }

    private fun finishPlaneDotFrame(
        anchorNode: SCNNode,
        anchor: ARPlaneAnchor,
        centerHit: CenterPlaneHit,
        stats: PlaneDotFrameStats
    ): PlaneDotFrameStats {
        val raycastWorldY = centerHit.confirmedHitResult?.let { HitTransformReader.worldFloorPoint(it).yMeters }
        planeDotElevationLock.apply(anchorNode, anchor, raycastWorldY)
        return stats
    }

    private fun updatePolygonStableFrames(anchor: ARPlaneAnchor) {
        val anchorId = anchor.identifier
        if (anchor.hasPolygonBoundary()) {
            val previous = polygonStableFrames[anchorId] ?: 0
            polygonStableFrames[anchorId] = minOf(previous + 1, 30)
        } else {
            polygonStableFrames.remove(anchorId)
        }
    }

    private fun hidePlaneDotVisualization(view: ARSCNView, frame: ARFrame) {
        view.scene?.rootNode?.let { hidePreviewDotGrid(it) }
        lastRenderedFocusedAnchorId?.let { focusedId ->
            frame.anchors
                .filterIsInstance<ARPlaneAnchor>()
                .firstOrNull { it.identifier == focusedId }
                ?.let { view.nodeForAnchor(it) }
                ?.let { hidePlaneDotGrid(it) }
        }
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

    private fun publishPlaneDebugMetrics(nowSeconds: Double, centerHit: CenterPlaneHit) {
        if (nowSeconds - lastMetricsPublishSeconds < METRICS_PUBLISH_INTERVAL_SECONDS) return
        lastMetricsPublishSeconds = nowSeconds
        onPlaneDebugMetricsChanged(
            IosPlaneDebugMetrics(
                fps = currentFps,
                rendererMode = lastPlaneFrameStats.rendererMode,
                dotCount = lastPlaneFrameStats.dotCount,
                nodeCount = lastPlaneFrameStats.nodeCount,
                bucketCount = lastPlaneFrameStats.bucketCount,
                generateMs = lastPlaneFrameStats.generateMs,
                syncMs = lastPlaneFrameStats.syncMs,
                previewLatencyMs = firstPreviewDotsMs,
                anchorLatencyMs = firstAnchorDotsMs,
                sessionFeatures = sessionFeatures.debugLabel(),
                hitPath = centerHit.hitPathDebugLabel()
            )
        )
    }

    private fun elapsedMsSinceSession(nowSeconds: Double): Int =
        ((nowSeconds - sessionStartTimeSeconds) * 1000.0).roundToInt()

    private fun buildFocusedLabel(
        focusedAnchorId: NSUUID?,
        dotCount: Int,
        inGracePeriod: Boolean
    ): String {
        if (focusedAnchorId == null) return "No"
        val suffix = if (inGracePeriod) " (hold)" else ""
        return "$dotCount pts$suffix"
    }

}

private data class IosPlaneDebugMetrics(
    val fps: Int = 0,
    val rendererMode: String = "none",
    val dotCount: Int = 0,
    val nodeCount: Int = 0,
    val bucketCount: Int = 0,
    val generateMs: Int = 0,
    val syncMs: Int = 0,
    val previewLatencyMs: Int? = null,
    val anchorLatencyMs: Int? = null,
    val sessionFeatures: String = "planes",
    val hitPath: String = "none"
)

private data class PlaneDotFrameStats(
    val rendererMode: String = "none",
    val dotCount: Int = 0,
    val nodeCount: Int = 0,
    val bucketCount: Int = 0,
    val generateMs: Int = 0,
    val syncMs: Int = 0
)

private const val METRICS_PUBLISH_INTERVAL_SECONDS = 0.25

private fun currentTimeSeconds(): Double =
    CFAbsoluteTimeGetCurrent()

private fun elapsedMs(startSeconds: Double): Int =
    ((currentTimeSeconds() - startSeconds) * 1000.0).roundToInt()

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
