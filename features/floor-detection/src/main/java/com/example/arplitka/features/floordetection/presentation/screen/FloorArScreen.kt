package com.example.arplitka.features.floordetection.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.arplitka.features.floordetection.domain.model.FloorDetectionState
import com.example.arplitka.features.floordetection.presentation.viewmodel.FloorArViewModel
import com.example.arplitka.shared.ui.BlockingMessage
import com.example.arplitka.shared.ui.CenterReticle
import com.example.arplitka.shared.ui.DebugPanel
import com.example.arplitka.shared.ui.StatusPanel
import com.google.ar.core.Config
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.math.Size
import io.github.sceneview.node.PlaneNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader

@Composable
fun FloorArScreen(
    viewModel: FloorArViewModel = hiltViewModel()
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val cameraNode = rememberARCameraNode(engine)
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var sessionError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewportSize = it }
    ) {
        ARSceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            cameraNode = cameraNode,
            planeRenderer = true,
            sessionConfiguration = { session, config ->
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    config.depthMode = Config.DepthMode.AUTOMATIC
                }
                config.focusMode = Config.FocusMode.AUTO
                config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            },
            onSessionFailed = { exception ->
                sessionError = exception.localizedMessage ?: "Не удалось запустить AR-сессию"
            },
            onSessionUpdated = { session, frame ->
                viewModel.onSessionUpdated(
                    session = session,
                    frame = frame,
                    viewportSize = viewportSize
                )
            }
        )

        CenterReticle(
            modifier = Modifier.align(Alignment.Center),
            isActive = viewModel.uiState.hasCenterHit
        )

        StatusPanel(
            statusText = viewModel.uiState.statusText,
            instructionText = viewModel.uiState.instructionText,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 36.dp)
        )

        DebugPanel(
            debugLines = mapOf(
                "Плоскости" to viewModel.uiState.horizontalPlaneCount.toString(),
                "Площадь" to "%.2f м²".format(viewModel.uiState.selectedArea),
                "Tracking" to viewModel.uiState.trackingState.name,
                "Depth API" to if (viewModel.uiState.isDepthEnabled) "On" else "Off",
                "Center hit" to if (viewModel.uiState.hasCenterHit) "yes" else "no"
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        )

        if (sessionError != null) {
            BlockingMessage(
                title = "AR недоступен",
                message = sessionError ?: "Не удалось запустить AR-сессию",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
