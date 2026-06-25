package com.example.arplitka.features.tiledetails.presentation

sealed interface TileDetailsEvent {
    data object OpenAr : TileDetailsEvent
    data object NavigateBack : TileDetailsEvent
}
