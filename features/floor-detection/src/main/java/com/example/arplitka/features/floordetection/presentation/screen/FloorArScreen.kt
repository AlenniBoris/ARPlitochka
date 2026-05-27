package com.example.arplitka.features.floordetection.presentation.screen

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.arplitka.features.floordetection.BuildConfig
import com.example.arplitka.features.floordetection.R
import com.example.arplitka.features.floordetection.presentation.components.ArActionButtons
import com.example.arplitka.features.floordetection.presentation.components.ArSceneLayer
import com.example.arplitka.features.floordetection.presentation.components.ArStatusOverlay
import com.example.arplitka.features.floordetection.presentation.components.ArTopBar
import com.example.arplitka.features.floordetection.presentation.viewmodel.FloorArViewModel
import com.example.arplitka.shared.ui.kit.BlockingMessage
import com.example.arplitka.shared.ui.kit.CenterReticle
import com.example.arplitka.shared.ui.kit.DebugPanel
import androidx.compose.foundation.layout.padding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FloorArScreen(
    viewModel: FloorArViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    BackHandler(onBack = onBack)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var sessionErrorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    var pavingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(context, uiState.selectedTileType) {
        runCatching {
            pavingBitmap = withContext(Dispatchers.IO) {
                context.assets.open(uiState.selectedTileType.assetPath).use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            }
        }.onFailure { 
            android.util.Log.e("FloorArScreen", "Async texture load failed", it)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewportSize = it }
    ) {
        ArSceneLayer(
            uiState = uiState,
            pavingBitmap = pavingBitmap,
            onSessionUpdated = { session, frame ->
                viewModel.onSessionUpdated(session, frame, viewportSize)
            },
            onSessionFailed = { exception ->
                sessionErrorMessage = exception.localizedMessage ?: "AR session failed"
            },
            onSizeChanged = { viewportSize = it }
        )

        CenterReticle(
            modifier = Modifier.align(Alignment.Center),
            isActive = uiState.hasCenterHit
        )

        ArTopBar(onBack = onBack)

        ArStatusOverlay(
            status = uiState.status,
            instruction = uiState.instruction,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        ArActionButtons(
            uiState = uiState,
            onAddPoint = { viewModel.addPoint() },
            onUndoPoint = { viewModel.undoPoint() },
            onRotateTexture = { viewModel.rotateTexture() },
            onToggleTile = { viewModel.toggleTileType() },
            onClearSection = { viewModel.clearSection() },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (BuildConfig.DEBUG) {
            DebugPanel(
                debugLines = mapOf(
                    stringResource(R.string.debug_planes) to uiState.horizontalPlaneCount.toString(),
                    stringResource(R.string.debug_area) to stringResource(R.string.area_format, uiState.selectedArea),
                    stringResource(R.string.debug_tracking) to uiState.trackingState.name,
                    "Points" to uiState.points.size.toString(),
                    "Closed" to uiState.isPolygonClosed.toString(),
                    "Finalized" to uiState.isFinalized.toString(),
                    "Texture rotation" to (uiState.textureRotation.ordinal * 45).toString()
                ),
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
            )
        }

        if (sessionErrorMessage != null) {
            BlockingMessage(
                title = stringResource(R.string.ar_not_available),
                message = sessionErrorMessage!!,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
