package com.example.arplitka.shared.tiles.domain.model

import com.example.arplitka.shared.core.domain.model.CommonException

sealed interface ArCatalogState {
    data object Initial : ArCatalogState
    data object Loading : ArCatalogState
    data class Content(val tiles: List<Tile>) : ArCatalogState
    data class Error(val exception: CommonException) : ArCatalogState
}
