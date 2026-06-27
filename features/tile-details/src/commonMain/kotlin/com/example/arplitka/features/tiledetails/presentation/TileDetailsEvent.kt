package com.example.arplitka.features.tiledetails.presentation

import com.example.arplitka.shared.tiles.domain.model.TileSelection

sealed interface TileDetailsEvent {
    data class OpenAr(val selection: TileSelection) : TileDetailsEvent
    data object NavigateBack : TileDetailsEvent
}
