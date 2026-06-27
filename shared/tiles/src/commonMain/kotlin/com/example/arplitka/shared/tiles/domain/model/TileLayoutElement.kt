package com.example.arplitka.shared.tiles.domain.model

data class TileLayoutElement(
    val elementTypeId: String,
    val name: String,
    val widthMm: Int,
    val heightMm: Int,
    val countInRepeat: Int,
    val colorSlotId: String,
    val colorOptions: List<TileElementColorOption>
)
