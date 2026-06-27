package com.example.arplitka.shared.tiles.domain.model

data class TilePalette(
    val id: String,
    val name: String,
    val textureUrl: String,
    val previewUrl: String? = null,
    val selectedColorsBySlot: Map<String, Long> = emptyMap()
)
