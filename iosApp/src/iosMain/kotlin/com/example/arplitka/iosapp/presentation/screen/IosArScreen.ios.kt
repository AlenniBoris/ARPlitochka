package com.example.arplitka.iosapp.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
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
import com.example.arplitka.shared.ui.kit.ArContourActionButtons
import com.example.arplitka.shared.ui.kit.ArTopBar
import com.example.arplitka.shared.ui.kit.CenterReticle
import com.example.arplitka.shared.ui.kit.DebugPanel
import com.example.arplitka.shared.ui.kit.StatusPanel
import com.example.arplitka.shared.ui.kit.isDebugBuild
import kotlinx.cinterop.ExperimentalForeignApi
import platform.ARKit.ARSCNView
import platform.CoreGraphics.CGRectMake
import kotlin.math.roundToInt

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun IosArScreen(onBack: () -> Unit) {
    val model = remember { IosArScreenModel() }
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
            state = contourState.toReticleState(placementHint, planeDebugMetrics.placementStatus)
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

            if (contourState.showContourActions) {
                ArContourActionButtons(
                    hasCenterHit = contourState.hasCenterHit || contourState.isPolygonClosed,
                    isPolygonClosed = contourState.isPolygonClosed,
                    hasPoints = contourState.placedPoints.isNotEmpty(),
                    onAddPoint = { coordinator.dispatchEvent(FloorArEvent.AddPoint) },
                    onUndoPoint = { coordinator.dispatchEvent(FloorArEvent.UndoPoint) },
                    addContentDescription = "Добавить точку",
                    undoContentDescription = "Отменить",
                    okContentDescription = "Готово"
                )
            }

            if (contourState.showTileControls) {
                if (contourState.isTileVisible) {
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
                                onClick = { coordinator.dispatchEvent(FloorArEvent.RotateTexture) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                )
                            ) {
                                Text("Повернуть")
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { coordinator.dispatchEvent(FloorArEvent.ToggleTileVisibility) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (contourState.isTileVisible) {
                                Color.Black.copy(alpha = 0.72f)
                            } else {
                                Color(0xFF4CAF50)
                            },
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(if (contourState.isTileVisible) "Убрать плитку" else "Добавить плитку")
                    }

                    if (contourState.isTileVisible) {
                        Button(
                            onClick = { coordinator.dispatchEvent(FloorArEvent.ChangeTileType) },
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
                    "Finalized" to if (contourState.isFinalized) "Yes" else "No",
                    "Tile" to if (contourState.isTileVisible) "On" else "Off",
                    "Texture rotation" to contourState.textureRotation.degrees.toString(),
                    "Tile type" to contourState.selectedTileType.resourceName,
                    "Phase" to planeDebugMetrics.sessionPhase,
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
                    "Track quality" to planeDebugMetrics.trackingQualityLabel,
                    "Hit Y" to planeDebugMetrics.hitYLabel,
                    "Largest plane" to "${(planeDebugMetrics.largestPlaneAreaM2 * 100).roundToInt() / 100.0} m²",
                    "Reloc" to planeDebugMetrics.relocLabel,
                    "Anchor corr" to planeDebugMetrics.anchorCorrectionLabel,
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
    }
}
