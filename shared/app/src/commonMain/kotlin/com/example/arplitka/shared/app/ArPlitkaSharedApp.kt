package com.example.arplitka.shared.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
                    SharedBottomBar(
                        currentRoute = currentRoute,
                        onCatalogClick = { currentRoute = SharedRoute.Catalog },
                        onArClick = { currentRoute = SharedRoute.Ar }
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
private fun SharedBottomBar(
    currentRoute: SharedRoute,
    onCatalogClick: () -> Unit,
    onArClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SharedBottomBarItem(
            text = "Каталог",
            isSelected = currentRoute == SharedRoute.Catalog,
            onClick = onCatalogClick
        )
        SharedBottomBarItem(
            text = "AR",
            isSelected = currentRoute == SharedRoute.Ar,
            onClick = onArClick
        )
    }
}

@Composable
private fun SharedBottomBarItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(if (isSelected) Color(0xFFE8F5E9) else Color.Transparent)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            color = if (isSelected) Color(0xFF2E7D32) else Color.Gray,
            style = MaterialTheme.typography.labelLarge
        )
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
