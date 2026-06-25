package com.example.arplitka.features.catalog.presentation.viewmodel

import com.example.arplitka.shared.tiles.domain.model.Tile
import com.example.arplitka.shared.core.domain.model.CommonException

sealed interface CatalogUiState {
    data object Loading : CatalogUiState
    data class Content(val tiles: List<Tile>) : CatalogUiState
    data class Error(val exception: CommonException) : CatalogUiState
}
