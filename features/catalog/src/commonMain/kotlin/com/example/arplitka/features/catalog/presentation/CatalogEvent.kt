package com.example.arplitka.features.catalog.presentation

sealed interface CatalogEvent {
    data class OpenTile(val id: Long) : CatalogEvent
}
