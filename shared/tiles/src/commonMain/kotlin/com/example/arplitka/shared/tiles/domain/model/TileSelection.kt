package com.example.arplitka.shared.tiles.domain.model

data class TileSelection(
    val tileId: Long,
    val layoutId: String,
    val paletteId: String,
    val selectedColorsBySlot: Map<String, Long> = emptyMap(),
    val variantId: Long? = null,
    val thicknessMm: Int? = null
)
