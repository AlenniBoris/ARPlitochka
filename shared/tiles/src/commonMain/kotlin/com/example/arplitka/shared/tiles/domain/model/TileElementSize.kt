package com.example.arplitka.shared.tiles.domain.model

data class TileElementSize(
    val widthMm: Int,
    val heightMm: Int,
    val label: String? = null,
    val quantityInPattern: Int? = null
)
