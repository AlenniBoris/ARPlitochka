package com.example.arplitka.shared.tiles.domain.model

data class TileLayout(
    val id: String,
    val name: String,
    val previewUrl: String? = null,
    val defaultTextureUrl: String,
    val repeatWidthMm: Int,
    val repeatHeightMm: Int,
    val elements: List<TileLayoutElement> = emptyList(),
    val palettes: List<TilePalette> = emptyList()
)
