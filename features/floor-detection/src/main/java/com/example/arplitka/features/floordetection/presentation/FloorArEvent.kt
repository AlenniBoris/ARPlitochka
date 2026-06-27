package com.example.arplitka.features.floordetection.presentation

sealed interface FloorArEvent {
    data class NavigateBack(val returnToTileId: Long?) : FloorArEvent
}
