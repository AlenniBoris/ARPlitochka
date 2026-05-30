package com.example.arplitka

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.arplitka.features.floordetection.presentation.screen.FloorArScreen
import com.example.arplitka.shared.app.ArPlitkaSharedApp
import com.example.arplitka.shared.ui.kit.BlockingMessage
import com.google.ar.core.ArCoreApk
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            ArPlitkaApp()
        }
    }
}

@Composable
private fun ArPlitkaApp() {
    ArPlitkaSharedApp(
        arContent = { onBack ->
            CameraPermissionGate {
                ArCoreAvailabilityGate {
                    FloorArScreen(onBack = onBack)
                }
            }
        }
    )
}

@Composable
private fun CameraPermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        content()
    } else {
        BlockingMessage(
            title = stringResource(R.string.permission_camera_title),
            message = stringResource(R.string.permission_camera_message),
            actionText = stringResource(R.string.permission_camera_button),
            onAction = { permissionLauncher.launch(Manifest.permission.CAMERA) }
        )
    }
}

@Composable
private fun ArCoreAvailabilityGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var availability by remember {
        mutableStateOf(ArCoreApk.getInstance().checkAvailability(context))
    }

    LaunchedEffect(Unit) {
        val checked = ArCoreApk.getInstance().checkAvailability(context)
        availability = checked
        if (checked.isTransient) {
            kotlinx.coroutines.delay(300)
            availability = ArCoreApk.getInstance().checkAvailability(context)
        }
    }

    when {
        availability.isSupported -> content()
        availability.isTransient -> BlockingMessage(
            title = stringResource(R.string.arcore_checking_title),
            message = stringResource(R.string.arcore_checking_message),
        )
        else -> BlockingMessage(
            title = stringResource(R.string.arcore_unsupported_title),
            message = stringResource(R.string.arcore_unsupported_message),
        )
    }
}
