package com.example.arplitka.shared.tiles.domain.model

data class TileColor(
    val id: Long,
    val name: String,
    val textureUrl: String,
    val hexCode: String,
    val swatchUrl: String? = null,
    val displayOrder: Int = 0
)
