package com.example.arplitka.features.catalog.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.arplitka.features.catalog.presentation.viewmodel.CatalogViewModel
import com.example.arplitka.features.catalog.presentation.viewmodel.CatalogUiState
import com.example.arplitka.shared.ui.core.model.toUiModel
import com.example.arplitka.shared.ui.kit.screens.AppProgressScreen
import com.example.arplitka.shared.ui.kit.screens.AppExceptionScreen
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
                AppExceptionScreen(
                    exception = currentState.exception.toUiModel(),
                    onTryAgain = { viewModel.loadTiles() }
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
