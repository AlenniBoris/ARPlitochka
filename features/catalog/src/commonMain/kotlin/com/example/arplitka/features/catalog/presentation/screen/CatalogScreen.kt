package com.example.arplitka.features.catalog.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.arplitka.features.catalog.presentation.viewmodel.CatalogViewModel
import com.example.arplitka.features.catalog.presentation.viewmodel.CatalogUiState
import com.example.arplitka.shared.ui.core.model.toUiModel
import com.example.arplitka.shared.ui.kit.components.AppRefreshIndicator
import com.example.arplitka.shared.ui.kit.screens.AppProgressScreen
import com.example.arplitka.shared.ui.kit.screens.AppExceptionScreen
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    onOpenAr: () -> Unit,
    viewModel: CatalogViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val refreshState = rememberPullToRefreshState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Каталог плитки",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            isRefreshing = false,
            onRefresh = { viewModel.refreshTiles() },
            state = refreshState,
            indicator = {
                AppRefreshIndicator(
                    modifier = Modifier.align(Alignment.TopCenter),
                    state = refreshState,
                    isRefreshing = false
                )
            }
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
                    CatalogList(
                        tiles = currentState.tiles,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
