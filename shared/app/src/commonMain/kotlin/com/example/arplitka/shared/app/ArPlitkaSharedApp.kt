package com.example.arplitka.shared.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.arplitka.shared.app.navigation.SharedRoute
import com.example.arplitka.shared.ui.navigation.AR_ROUTE
import com.example.arplitka.shared.ui.navigation.AppBottomBar
import com.example.arplitka.shared.ui.navigation.BottomBarValues
import com.example.arplitka.shared.ui.navigation.CATALOG_ROUTE
import com.example.arplitka.shared.ui.navigation.toModelUi
import kotlinx.coroutines.delay

@Composable
fun ArPlitkaSharedApp(
    catalogContent: @Composable (onOpenAr: () -> Unit) -> Unit,
    arContent: @Composable (onBack: () -> Unit) -> Unit = { onBack ->
        SharedArPlaceholderScreen(onBack = onBack)
    }
) {
    var currentRoute by remember { mutableStateOf<SharedRoute>(SharedRoute.Catalog) }

    MaterialTheme {
        Scaffold(
            bottomBar = {
                if (currentRoute == SharedRoute.Catalog) {
                    val items = remember {
                        listOf(
                            BottomBarValues.Catalog.toModelUi(onClick = { currentRoute = SharedRoute.Catalog }),
                            BottomBarValues.AR.toModelUi(onClick = { currentRoute = SharedRoute.Ar })
                        )
                    }
                    AppBottomBar(
                        items = items,
                        currentRoute = when (currentRoute) {
                            SharedRoute.Catalog -> CATALOG_ROUTE
                            SharedRoute.Ar -> AR_ROUTE
                            else -> null
                        }
                    )
                }
            }
        ) { paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (currentRoute == SharedRoute.Catalog) paddingValues else PaddingValues()),
                color = if (currentRoute == SharedRoute.Ar) Color.Black else Color.White
            ) {
                when (currentRoute) {
                    SharedRoute.Catalog -> catalogContent {
                        currentRoute = SharedRoute.Ar
                    }
                    SharedRoute.Ar -> arContent {
                        currentRoute = SharedRoute.Transition
                    }
                    SharedRoute.Transition -> SharedTransitionScreen(
                        onComplete = {
                            currentRoute = SharedRoute.Catalog
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SharedTransitionScreen(onComplete: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(400)
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
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
