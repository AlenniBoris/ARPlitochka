package com.example.arplitka.features.catalog.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.arplitka.features.catalog.presentation.viewmodel.CatalogViewModel
import com.example.arplitka.features.catalog.presentation.viewmodel.CatalogUiState
import com.example.arplitka.shared.ui.core.mapper.toUiString
import com.example.arplitka.shared.ui.kit.AppProgressScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CatalogScreen(
    onOpenAr: () -> Unit,
    viewModel: CatalogViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        when (val currentState = state) {
            is CatalogUiState.Loading -> {
                AppProgressScreen()
            }
            is CatalogUiState.Error -> {
                ErrorState(
                    message = currentState.exception.toUiString(),
                    onRetry = { viewModel.loadTiles() },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is CatalogUiState.Content -> {
                CatalogContent(
                    tiles = currentState.tiles,
                    onOpenAr = onOpenAr
                )
            }
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
    ) {
        androidx.compose.material3.Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        androidx.compose.material3.Button(onClick = onRetry) {
            androidx.compose.material3.Text("Повторить")
        }
    }
}
