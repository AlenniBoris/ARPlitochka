package com.example.arplitka

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.arplitka.features.catalog.presentation.screen.CatalogScreen
import com.example.arplitka.features.floordetection.presentation.screen.FloorArScreen
import com.example.arplitka.shared.ui.kit.BlockingMessage
import com.example.arplitka.shared.ui.navigation.AR_ROUTE
import com.example.arplitka.shared.ui.navigation.AppBottomBar
import com.example.arplitka.shared.ui.navigation.BOTTOM_BAR_ROUTES
import com.example.arplitka.shared.ui.navigation.BottomBarValues
import com.example.arplitka.shared.ui.navigation.CATALOG_ROUTE
import com.example.arplitka.shared.ui.navigation.TRANSITION_ROUTE
import com.example.arplitka.shared.ui.navigation.toModelUi
import com.google.ar.core.ArCoreApk
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

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
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val shouldShowBottomBar = currentRoute in BOTTOM_BAR_ROUTES

    MaterialTheme {
        Scaffold(
            bottomBar = {
                if (shouldShowBottomBar) {
                    AppBottomBar(
                        items = BottomBarValues.entries.map { value ->
                            value.toModelUi(
                                onClick = {
                                    navController.navigate(value.toModelUi().route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        },
                        currentRoute = currentRoute
                    )
                }
            }
        ) { paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (shouldShowBottomBar) paddingValues else PaddingValues()),
                color = if (currentRoute == AR_ROUTE || currentRoute == TRANSITION_ROUTE) Color.Black else Color.White
            ) {
                NavHost(
                    navController = navController,
                    startDestination = CATALOG_ROUTE,
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None }
                ) {
                    composable(CATALOG_ROUTE) {
                        CatalogScreen()
                    }
                    composable(AR_ROUTE) {
                        CameraPermissionGate {
                            ArCoreAvailabilityGate {
                                FloorArScreen(
                                    onBack = { 
                                        navController.navigate(TRANSITION_ROUTE) {
                                            popUpTo(CATALOG_ROUTE) { inclusive = false }
                                        }
                                    }
                                )
                            }
                        }
                    }
                    composable(TRANSITION_ROUTE) {
                        TransitionScreen(
                            onComplete = {
                                navController.navigate(CATALOG_ROUTE) {
                                    popUpTo(TRANSITION_ROUTE) { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransitionScreen(onComplete: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(400) // Даем время на корректное закрытие AR-сессии
        onComplete()
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
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
