package com.example.arplitka.features.tiledetails.presentation.screen

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.arplitka.features.tiledetails.presentation.TileDetailsEvent
import com.example.arplitka.features.tiledetails.presentation.viewmodel.TileDetailsViewModel
import com.example.arplitka.shared.ui.kit.components.BackHandler
import com.example.arplitka.shared.ui.navigation.AppNavigator
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TileDetailsScreen(
    tileId: Long,
    navigator: AppNavigator,
    viewModel: TileDetailsViewModel = koinViewModel(
        parameters = { parametersOf(tileId) }
    )
) {
    val state by viewModel.state.collectAsState()
    val refreshState = rememberPullToRefreshState()
    val event by remember { mutableStateOf(viewModel.event) }

    LaunchedEffect(event) {
        launch {
            event.filterIsInstance<TileDetailsEvent.OpenAr>()
                .collect { navigator.openAr() }
        }
        launch {
            event.filterIsInstance<TileDetailsEvent.NavigateBack>()
                .collect { navigator.back() }
        }
    }

    BackHandler {
        viewModel.onBack()
    }

    TileDetailsContent(
        state = state,
        onBack = viewModel::onBack,
        onTryInAr = viewModel::onTryInAr,
        onRefresh = viewModel::loadTile,
        refreshState = refreshState
    )
}
