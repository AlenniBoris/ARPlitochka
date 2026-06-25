package com.example.arplitka.features.floordetection.presentation

sealed interface FloorArEvent {
    data object NavigateBack : FloorArEvent
}
