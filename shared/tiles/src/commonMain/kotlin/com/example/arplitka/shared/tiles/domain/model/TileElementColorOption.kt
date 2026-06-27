package com.example.arplitka.shared.tiles.domain.model

data class TileElementColorOption(
    val colorId: Long,
    val name: String,
    val hexCode: String,
    val textureUrl: String? = null,
    val swatchUrl: String? = null,
    val sku: String? = null
)
