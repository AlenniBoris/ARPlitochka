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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.arplitka.features.floordetection.BuildConfig
import com.example.arplitka.features.floordetection.R
import com.example.arplitka.features.floordetection.presentation.viewmodel.FloorArViewModel
import com.example.arplitka.shared.ui.BlockingMessage
import com.example.arplitka.shared.ui.CenterReticle
import com.example.arplitka.shared.ui.DebugPanel
import com.example.arplitka.shared.ui.StatusPanel
import com.example.arplitka.shared.ui.UiText
import com.google.ar.core.Config
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.rememberARCameraNode
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
    var sessionError by remember { mutableStateOf<UiText?>(null) }

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
                sessionError = UiText.DynamicString(
                    exception.localizedMessage ?: "AR session failed"
                )
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
            statusText = viewModel.uiState.statusText.asString(),
            instructionText = viewModel.uiState.instructionText.asString(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 36.dp)
        )

        if (BuildConfig.DEBUG) {
            DebugPanel(
                debugLines = mapOf(
                    stringResource(R.string.debug_planes) to viewModel.uiState.horizontalPlaneCount.toString(),
                    stringResource(R.string.debug_area) to stringResource(R.string.area_format, viewModel.uiState.selectedArea),
                    stringResource(R.string.debug_tracking) to viewModel.uiState.trackingState.name,
                    stringResource(R.string.debug_depth_api) to if (viewModel.uiState.isDepthEnabled) stringResource(R.string.on) else stringResource(R.string.off),
                    stringResource(R.string.debug_center_hit) to if (viewModel.uiState.hasCenterHit) stringResource(R.string.yes) else stringResource(R.string.no)
                ),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            )
        }

        if (sessionError != null) {
            BlockingMessage(
                title = stringResource(R.string.ar_not_available),
                message = sessionError?.asString() ?: stringResource(R.string.ar_session_failed),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
