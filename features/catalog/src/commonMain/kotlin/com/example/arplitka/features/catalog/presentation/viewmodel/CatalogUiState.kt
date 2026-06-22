package com.example.arplitka.features.catalog.presentation.viewmodel

import com.example.arplitka.shared.core.domain.model.CommonException
import com.example.arplitka.shared.tiles.domain.model.Tile

data class CatalogUiState(
    val isLoading: Boolean = false,
    val tiles: List<Tile> = emptyList(),
    val error: CommonException? = null
)
