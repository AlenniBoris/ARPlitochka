package com.example.arplitka.shared.app

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.arplitka.features.catalog.presentation.screen.CatalogScreen
import com.example.arplitka.features.tiledetails.presentation.screen.TileDetailsScreen
import com.example.arplitka.shared.ui.navigation.ArRoute
import com.example.arplitka.shared.ui.navigation.AppBottomBar
import com.example.arplitka.shared.ui.navigation.AppNavigator
import com.example.arplitka.shared.tiles.domain.model.TileSelection
import com.example.arplitka.shared.ui.navigation.toArNavigationArgs
import com.example.arplitka.shared.ui.navigation.BottomBarValues
import com.example.arplitka.shared.ui.navigation.CATALOG_ROUTE
import com.example.arplitka.shared.ui.navigation.CatalogRoute
import com.example.arplitka.shared.ui.navigation.TileDetailsRoute
import com.example.arplitka.shared.ui.navigation.TransitionToArRoute
import com.example.arplitka.shared.ui.navigation.toModelUi
import kotlinx.coroutines.delay

@Composable
fun ArPlitkaSharedApp(
    arContent: @Composable (navigator: AppNavigator, arRoute: ArRoute) -> Unit = { navigator, _ ->
        SharedArPlaceholderScreen(onBack = { navigator.backFromAr() })
    }
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isCatalogScreen = currentDestination?.hasRoute<CatalogRoute>() == true
    val isArScreen = currentDestination?.hasRoute<ArRoute>() == true

    val navigator = remember(navController) {
        object : AppNavigator {
            override fun openCatalog() {
                if (navController.currentBackStackEntry?.destination?.hasRoute<CatalogRoute>() == true) return
                navController.navigate(CatalogRoute) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            }

            override fun openTile(id: Long) {
                val currentEntry = navController.currentBackStackEntry
                if (currentEntry?.destination?.hasRoute<TileDetailsRoute>() == true) {
                    val currentTileId = currentEntry.toRoute<TileDetailsRoute>().tileId
                    if (currentTileId == id) return
                }
                navController.navigate(TileDetailsRoute(tileId = id)) {
                    launchSingleTop = true
                }
            }

            override fun openAr(selection: TileSelection?) {
                if (navController.currentBackStackEntry?.destination?.hasRoute<ArRoute>() == true) return
                val args = selection?.toArNavigationArgs()
                navController.navigate(
                    TransitionToArRoute(
                        tileId = args?.tileId,
                        layoutId = args?.layoutId,
                        paletteId = args?.paletteId
                    )
                )
            }

            override fun back() {
                navController.popBackStack()
            }

            override fun backFromAr(returnToTileId: Long?) {
                val onArScreen =
                    navController.currentBackStackEntry?.destination?.hasRoute<ArRoute>() == true
                if (!onArScreen) {
                    navController.popBackStack()
                    return
                }

                if (returnToTileId != null) {
                    val launchedFromTileDetails =
                        navController.previousBackStackEntry?.destination?.hasRoute<TileDetailsRoute>() == true
                    navController.navigate(TileDetailsRoute(tileId = returnToTileId)) {
                        if (launchedFromTileDetails) {
                            popUpTo<TileDetailsRoute> { inclusive = true }
                        } else {
                            popUpTo<ArRoute> { inclusive = true }
                        }
                    }
                } else {
                    navController.navigate(CatalogRoute) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    MaterialTheme {
        Scaffold(
            bottomBar = {
                if (isCatalogScreen) {
                    val items = remember {
                        listOf(
                            BottomBarValues.Catalog.toModelUi(onClick = { navigator.openCatalog() }),
                            BottomBarValues.AR.toModelUi(onClick = { navigator.openAr() })
                        )
                    }
                    AppBottomBar(
                        items = items,
                        currentRoute = CATALOG_ROUTE
                    )
                }
            }
        ) { paddingValues ->
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = if (isArScreen) Color.Black else Color.White
            ) {
                NavHost(
                    navController = navController,
                    startDestination = CatalogRoute,
                    modifier = Modifier.padding(
                        if (isCatalogScreen) paddingValues else PaddingValues()
                    ),
                    enterTransition = { fadeIn(animationSpec = tween(200)) },
                    exitTransition = { fadeOut(animationSpec = tween(200)) },
                    popEnterTransition = { fadeIn(animationSpec = tween(200)) },
                    popExitTransition = { fadeOut(animationSpec = tween(200)) }
                ) {
                    composable<CatalogRoute> {
                        CatalogScreen(navigator = navigator)
                    }

                    composable<TransitionToArRoute> { backStackEntry ->
                        val transitionRoute = backStackEntry.toRoute<TransitionToArRoute>()
                        SharedTransitionScreen(
                            onComplete = {
                                navController.navigate(
                                    ArRoute(
                                        tileId = transitionRoute.tileId,
                                        layoutId = transitionRoute.layoutId,
                                        paletteId = transitionRoute.paletteId
                                    )
                                ) {
                                    popUpTo<TransitionToArRoute> { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable<TileDetailsRoute> { backStackEntry ->
                        val route = backStackEntry.toRoute<TileDetailsRoute>()
                        TileDetailsScreen(tileId = route.tileId, navigator = navigator)
                    }

                    composable<ArRoute> { backStackEntry ->
                        val arRoute = backStackEntry.toRoute<ArRoute>()
                        arContent(navigator, arRoute)
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedTransitionScreen(onComplete: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(100)
        onComplete()
    }
    Box(modifier = Modifier.fillMaxSize().background(Color.White))
}

@Composable
private fun SharedArPlaceholderScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Platform AR",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Android uses ARCore. iOS uses ARKit.",
                color = Color.White
            )
            Button(onClick = onBack) {
                Text("Назад")
            }
        }
    }
}
