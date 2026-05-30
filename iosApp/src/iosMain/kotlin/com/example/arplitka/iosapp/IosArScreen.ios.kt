package com.example.arplitka.iosapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
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
import com.example.arplitka.shared.ui.kit.CenterReticle
import com.example.arplitka.shared.ui.kit.DebugPanel
import com.example.arplitka.shared.ui.kit.StatusPanel
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
    var trackingStatus by remember { mutableStateOf(ArTrackingStatus.INITIALIZING) }
    var instruction by remember { mutableStateOf(ArInstruction.PLEASE_WAIT) }
    var hasCenterHit by remember { mutableStateOf(false) }

    var planeCount by remember { mutableStateOf(0) }
    var floorArea by remember { mutableStateOf(0f) }
    var trackingStateName by remember { mutableStateOf("INITIALIZING") }
    var focusedLabel by remember { mutableStateOf("No") }

    val coordinator = remember {
        IosArSessionCoordinator(
            onTrackingChanged = { status, nextInstruction, trackingName ->
                trackingStatus = status
                instruction = nextInstruction
                trackingStateName = trackingName
            },
            onFrameUpdated = { horizontalPlaneCount, centerHit, floorDetected, selectedArea, nextFocusedLabel ->
                planeCount = horizontalPlaneCount
                hasCenterHit = centerHit
                floorArea = selectedArea
                focusedLabel = nextFocusedLabel
                when {
                    trackingStatus == ArTrackingStatus.TRACKING_LOST -> Unit
                    floorDetected -> {
                        trackingStatus = ArTrackingStatus.FLOOR_DETECTED
                        instruction = ArInstruction.DETECTED
                    }
                    trackingStatus == ArTrackingStatus.FLOOR_DETECTED -> {
                        trackingStatus = ArTrackingStatus.SEARCHING_FLOOR
                        instruction = ArInstruction.SEARCHING
                    }
                }
            }
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
            statusText = trackingStatus.toIosText(),
            instructionText = instruction.toIosText(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 36.dp)
        )

        CenterReticle(
            modifier = Modifier.align(Alignment.Center),
            isActive = hasCenterHit
        )

        DebugPanel(
            debugLines = mapOf(
                "Planes" to planeCount.toString(),
                "Focused" to focusedLabel,
                "Area" to "${(floorArea * 100).roundToInt() / 100.0} m²",
                "Tracking" to trackingStateName,
                "Center hit" to if (hasCenterHit) "Yes" else "No"
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .padding(bottom = 100.dp)
        )

        Button(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp)
        ) {
            Text("Назад")
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class IosArSessionCoordinator(
    private val onTrackingChanged: (ArTrackingStatus, ArInstruction, String) -> Unit,
    private val onFrameUpdated: (
        horizontalPlaneCount: Int,
        centerHit: Boolean,
        floorDetected: Boolean,
        selectedArea: Float,
        focusedLabel: String
    ) -> Unit
) : NSObject(), ARSCNViewDelegateProtocol, ARSessionDelegateProtocol {

    private var sceneView: ARSCNView? = null
    private var lastPlaneCount: Int = -1
    private var lastConfirmedCenterHit: Boolean = false
    private var lastFloorDetected: Boolean = false
    private var lastSelectedArea: Float = -1f
    private var lastFocusedLabel: String = ""
    private var floorDotMaterial = createFloorDotMaterial()
    private val planeFingerprints: PlaneFingerprints = mutableMapOf()
    private val planeAreas: PlaneAreas = mutableMapOf()
    private val planeDotCounts: PlaneDotCounts = mutableMapOf()
    private val focusedPlaneTracker = FocusedPlaneTracker()
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
        floorDotMaterial = createFloorDotMaterial()
        sceneView.debugOptions = 0UL
        sceneView.delegate = this
        sceneView.session.delegate = this
        sceneView.session.runWithConfiguration(
            createConfiguration(),
            ARSessionRunOptionResetTracking or ARSessionRunOptionRemoveExistingAnchors
        )
    }

    fun pause() {
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
            onFrameUpdated(
                horizontalPlanes.size,
                hasVisualCenterHit,
                floorDetected,
                selectedArea,
                focusedLabel
            )
        }
    }

    override fun session(session: ARSession, cameraDidChangeTrackingState: ARCamera) {
        when (cameraDidChangeTrackingState.trackingState) {
            ARTrackingState.ARTrackingStateNotAvailable -> onTrackingChanged(
                ArTrackingStatus.TRACKING_LOST,
                ArInstruction.MOVE_PHONE,
                "NOT_AVAILABLE"
            )
            ARTrackingState.ARTrackingStateLimited -> onTrackingChanged(
                ArTrackingStatus.SEARCHING_FLOOR,
                ArInstruction.MOVE_PHONE,
                "LIMITED"
            )
            ARTrackingState.ARTrackingStateNormal -> onTrackingChanged(
                ArTrackingStatus.SEARCHING_FLOOR,
                ArInstruction.SEARCHING,
                "TRACKING"
            )
            else -> onTrackingChanged(
                ArTrackingStatus.SEARCHING_FLOOR,
                ArInstruction.SEARCHING,
                "UNKNOWN"
            )
        }
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
    ArInstruction.EMPTY -> ""
}
