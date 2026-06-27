package com.example.arplitka.features.tiledetails.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.arplitka.features.tiledetails.presentation.screen.components.TileDetailsBottomBar
import com.example.arplitka.features.tiledetails.presentation.model.TileLayoutOptionUi
import com.example.arplitka.features.tiledetails.presentation.model.TilePaletteOptionUi
import com.example.arplitka.features.tiledetails.presentation.model.TileThicknessOptionUi
import com.example.arplitka.features.tiledetails.presentation.viewmodel.TileDetailsUiState
import com.example.arplitka.shared.ui.core.model.toUiModel
import com.example.arplitka.shared.ui.kit.components.AppRefreshIndicator
import com.example.arplitka.shared.ui.kit.components.AppTopBar
import com.example.arplitka.shared.ui.kit.screens.AppExceptionScreen
import com.example.arplitka.shared.ui.kit.screens.AppProgressScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TileDetailsContent(
    state: TileDetailsUiState,
    onBack: () -> Unit,
    onTryInAr: () -> Unit,
    onOpenWebsite: () -> Unit,
    onLayoutSelected: (TileLayoutOptionUi) -> Unit,
    onPaletteSelected: (TilePaletteOptionUi) -> Unit,
    onThicknessSelected: (TileThicknessOptionUi) -> Unit,
    onToggleDescriptionExpanded: () -> Unit,
    onRefresh: () -> Unit,
    refreshState: PullToRefreshState
) {
    Scaffold(
        bottomBar = {
            if (state is TileDetailsUiState.Content) {
                TileDetailsBottomBar(
                    onOpenWebsite = onOpenWebsite,
                    onTryInAr = onTryInAr
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            AppTopBar(
                title = "",
                leftIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onLeftClick = onBack,
                rightIcon = Icons.Filled.AddShoppingCart,
                onRightClick = {}
            )

            PullToRefreshBox(
                modifier = Modifier.weight(1f),
                isRefreshing = false,
                onRefresh = onRefresh,
                state = refreshState,
                indicator = {
                    AppRefreshIndicator(
                        modifier = Modifier.align(Alignment.TopCenter),
                        state = refreshState,
                        isRefreshing = false
                    )
                }
            ) {
                TileDetailsStateContent(
                    state = state,
                    onRefresh = onRefresh,
                    onLayoutSelected = onLayoutSelected,
                    onPaletteSelected = onPaletteSelected,
                    onThicknessSelected = onThicknessSelected,
                    onToggleDescriptionExpanded = onToggleDescriptionExpanded,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun TileDetailsStateContent(
    state: TileDetailsUiState,
    onRefresh: () -> Unit,
    onLayoutSelected: (TileLayoutOptionUi) -> Unit,
    onPaletteSelected: (TilePaletteOptionUi) -> Unit,
    onThicknessSelected: (TileThicknessOptionUi) -> Unit,
    onToggleDescriptionExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when (state) {
            is TileDetailsUiState.Loading -> AppProgressScreen()
            is TileDetailsUiState.Error -> AppExceptionScreen(
                exception = state.exception.toUiModel(),
                onTryAgain = onRefresh
            )
            is TileDetailsUiState.Content -> TileDetailsInfo(
                state = state,
                onLayoutSelected = onLayoutSelected,
                onPaletteSelected = onPaletteSelected,
                onThicknessSelected = onThicknessSelected,
                onToggleDescriptionExpanded = onToggleDescriptionExpanded,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
