package com.example.arplitka.features.tiledetails.presentation.viewmodel

import com.example.arplitka.shared.tiles.domain.model.Tile
import com.example.arplitka.shared.core.domain.model.CommonException

sealed interface TileDetailsUiState {
    data object Loading : TileDetailsUiState
    data class Content(val tile: Tile) : TileDetailsUiState
    data class Error(val exception: CommonException) : TileDetailsUiState
}
