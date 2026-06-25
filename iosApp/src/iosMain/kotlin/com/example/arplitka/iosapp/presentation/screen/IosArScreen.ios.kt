package com.example.arplitka.iosapp.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.arplitka.iosapp.presentation.mapper.toIosText
import com.example.arplitka.iosapp.presentation.mapper.toReticleState
import com.example.arplitka.iosapp.presentation.mapper.toStatusDetailText
import com.example.arplitka.iosapp.presentation.model.IosArScreenModel
import com.example.arplitka.shared.ar.contracts.state.FloorArEvent
import com.example.arplitka.shared.ar.domain.model.FloorContourUiState
import com.example.arplitka.shared.ar.domain.model.FloorWorkflowStage
import com.example.arplitka.shared.ar.domain.model.TextureRotation
import com.example.arplitka.shared.ar.domain.model.TileType
import com.example.arplitka.shared.ui.kit.ar.ArContourActionButtons
import com.example.arplitka.shared.ui.kit.ar.ArTopBar
import com.example.arplitka.shared.ui.kit.ar.CenterReticle
import com.example.arplitka.shared.ui.kit.ar.DebugPanel
import com.example.arplitka.shared.ui.kit.ar.StatusPanel
import com.example.arplitka.shared.ui.kit.utils.isDebugBuild
import kotlinx.cinterop.ExperimentalForeignApi
import platform.ARKit.ARSCNView
import platform.CoreGraphics.CGRectMake
import kotlin.math.roundToInt

import com.example.arplitka.shared.ui.navigation.AppNavigator

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun IosArScreen(navigator: AppNavigator) {
    val model = remember { IosArScreenModel() }
    var showDebugPanel by remember { mutableStateOf(true) }
    val coordinator = model.coordinator
    val contourState = model.contourState
    val trackingStateName = model.trackingStateName
    val planeDebugMetrics = model.planeDebugMetrics
    val placementHint = model.placementHint
    val showContourRealignButton = model.showContourRealignButton
    val contourRealignPrompt = if (showContourRealignButton) {
        "Контур мог сместиться — нажмите «Выровнять», если точки не на полу"
    } else {
        null
    }

    DisposableEffect(model) {
        onDispose {
            model.pause()
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
            instructionText = when {
                contourState.snappedPointIndex != null -> "Отведите прицел от точки"
                contourRealignPrompt != null -> contourRealignPrompt
                else -> placementHint ?: contourState.instruction.toIosText()
            },
            detailText = contourState.toStatusDetailText(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 36.dp)
        )

        CenterReticle(
            modifier = Modifier.align(Alignment.Center),
            isActive = contourState.hasCenterHit && contourState.showContourActions,
            state = contourState.toReticleState(
                placementHint = placementHint,
                placementStatus = planeDebugMetrics.placementStatus,
                isPlacementPlaceable = planeDebugMetrics.isPlacementPlaceable
            )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showContourRealignButton) {
                Button(
                    onClick = { coordinator.applyContourRealignment() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Выровнять контур")
                }
            }

            when (contourState.stage) {
                FloorWorkflowStage.INITIALIZING,
                FloorWorkflowStage.SEARCHING_FLOOR -> {
                    // No buttons
                }

                FloorWorkflowStage.PLACEMENT_EMPTY,
                FloorWorkflowStage.PLACEMENT_ACTIVE -> {
                    ArContourActionButtons(
                        hasCenterHit = contourState.hasCenterHit,
                        isPolygonClosed = false,
                        hasPoints = contourState.placedPoints.isNotEmpty(),
                        onAddPoint = { coordinator.dispatchEvent(FloorArEvent.AddPoint) },
                        onUndoPoint = { coordinator.dispatchEvent(FloorArEvent.UndoPoint) },
                        addContentDescription = "Добавить точку",
                        undoContentDescription = "Отменить",
                        okContentDescription = "Готово"
                    )
                }

                FloorWorkflowStage.CONTOUR_CLOSED -> {
                    ArContourActionButtons(
                        hasCenterHit = true,
                        isPolygonClosed = true,
                        hasPoints = true,
                        onAddPoint = { coordinator.dispatchEvent(FloorArEvent.AddPoint) },
                        onUndoPoint = { coordinator.dispatchEvent(FloorArEvent.UndoPoint) },
                        addContentDescription = "Добавить точку",
                        undoContentDescription = "Отменить",
                        okContentDescription = "Готово"
                    )
                }

                FloorWorkflowStage.CONTOUR_CONFIRMED -> {
                    Button(
                        onClick = { coordinator.dispatchEvent(FloorArEvent.ToggleTileVisibility) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Добавить плитку")
                    }
                }

                FloorWorkflowStage.TILE_LAYOUT -> {
                    IosTileLayoutControls(
                        contourState = contourState,
                        onRotate = { coordinator.dispatchEvent(FloorArEvent.RotateTexture) },
                        onToggleVisibility = { coordinator.dispatchEvent(FloorArEvent.ToggleTileVisibility) },
                        onChangeType = { coordinator.dispatchEvent(FloorArEvent.ChangeTileType) }
                    )
                }
            }

            val canRescan = contourState.placedPoints.isNotEmpty() || 
                    contourState.isFinalized ||
                    contourState.stage == FloorWorkflowStage.PLACEMENT_EMPTY ||
                    contourState.stage == FloorWorkflowStage.SEARCHING_FLOOR ||
                    contourState.stage == FloorWorkflowStage.INITIALIZING

            if (canRescan) {
                Button(
                    onClick = { coordinator.rescanSession() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.92f),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Пересканировать")
                }
            }
        }

        if (isDebugBuild()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .padding(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showDebugPanel) {
                    DebugPanel(
                        debugLines = mapOf(
                            "Planes" to contourState.horizontalPlaneCount.toString(),
                            "Focused" to contourState.focusedLabel,
                            "AR plane" to "${(contourState.selectedArea * 100).roundToInt() / 100.0} m²",
                            "Tracking" to trackingStateName,
                            "Center hit" to if (contourState.hasCenterHit) "Yes" else "No",
                            "Points" to contourState.placedPoints.size.toString(),
                            "Closed" to if (contourState.isPolygonClosed) "Yes" else "No",
                            "Finalized" to if (contourState.isFinalized) "Yes" else "No",
                            "Tile" to if (contourState.isTileVisible) "On" else "Off",
                            "Texture rotation" to contourState.textureRotation.degrees.toString(),
                            "Tile type" to contourState.selectedTileType.resourceName,
                            "Phase" to planeDebugMetrics.sessionPhase,
                            "Stage" to contourState.stage.name,
                            "Perf" to planeDebugMetrics.perfDiagnosis,
                            "Plane renderer" to planeDebugMetrics.rendererMode,
                            "Delegate Hz" to planeDebugMetrics.sessionDelegateHz.toString(),
                            "Camera gap" to planeDebugMetrics.cameraFrameGapLabel,
                            "Delegate gap" to planeDebugMetrics.delegateWallGapLabel,
                            "Frame work" to "${planeDebugMetrics.frameHandleMs} ms",
                            "Renderer Hz" to planeDebugMetrics.rendererNodeCallbackHz.toString(),
                            "Surface overlays" to planeDebugMetrics.overlayCount.toString(),
                            "AR features" to planeDebugMetrics.sessionFeatures,
                            "Hit path" to planeDebugMetrics.hitPath,
                            "Detect gate" to planeDebugMetrics.detectGate,
                            "Scan patch" to planeDebugMetrics.scanPatch,
                            "Snap" to if (contourState.snappedPointIndex != null) "yes" else "no",
                            "Placement" to planeDebugMetrics.placementStatus,
                            "Hit age" to "${planeDebugMetrics.hitAgeMs} ms",
                            "Reticle age" to planeDebugMetrics.reticleHitAgeLabel,
                            "Reticle src" to planeDebugMetrics.reticleSourceLabel,
                            "Tap frame age" to planeDebugMetrics.tapFrameAgeLabel,
                            "Tap src" to planeDebugMetrics.tapSourceLabel,
                            "Tap Δ" to planeDebugMetrics.tapDeltaLabel,
                            "Tap reject" to planeDebugMetrics.tapRejectReason,
                            "Placement id" to if (planeDebugMetrics.placementSnapshotId >= 0) {
                                planeDebugMetrics.placementSnapshotId.toString()
                            } else {
                                "-"
                            },
                            "Placement age" to planeDebugMetrics.placementSnapshotAgeLabel,
                            "Tap snapshot" to if (planeDebugMetrics.tapSnapshotId >= 0) {
                                planeDebugMetrics.tapSnapshotId.toString()
                            } else {
                                "-"
                            },
                            "Placeable" to if (planeDebugMetrics.isPlacementPlaceable) "Yes" else "No",
                            "Contour ver" to planeDebugMetrics.contourVersion.toString(),
                            "Contour src" to planeDebugMetrics.contourSyncSource,
                            "Manual align" to if (planeDebugMetrics.manualAlignEligible) "Yes" else "No",
                            "Pending corr" to planeDebugMetrics.pendingCorrectionFrames.toString(),
                            "Track quality" to planeDebugMetrics.trackingQualityLabel,
                            "Hit Y" to planeDebugMetrics.hitYLabel,
                            "Largest plane" to "${(planeDebugMetrics.largestPlaneAreaM2 * 100).roundToInt() / 100.0} m²",
                            "Reloc" to planeDebugMetrics.relocLabel,
                            "Anchor corr" to planeDebugMetrics.anchorCorrectionLabel,
                            "Cull" to planeDebugMetrics.cullLabel
                        )
                    )
                }
                
                Button(
                    onClick = { showDebugPanel = !showDebugPanel },
                    modifier = Modifier.heightIn(min = 32.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (showDebugPanel) "Hide Debug" else "Show Debug",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        ArTopBar(
            onBack = { navigator.back() },
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
}

@Composable
private fun IosTileLayoutControls(
    contourState: FloorContourUiState,
    onRotate: () -> Unit,
    onToggleVisibility: () -> Unit,
    onChangeType: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(18.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Поворот текстуры: ${contourState.textureRotation.degrees}°",
                    color = Color.White
                )
                Button(
                    onClick = onRotate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Повернуть")
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onToggleVisibility,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black.copy(alpha = 0.72f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Убрать плитку")
            }

            Button(
                onClick = onChangeType,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.92f),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Сменить плитку")
            }
        }
    }
}
