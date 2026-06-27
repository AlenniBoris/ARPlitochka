package com.example.arplitka.shared.tiles.domain.model

data class TileEstimateLine(
    val sku: String,
    val name: String,
    val elementTypeId: String,
    val colorId: Long,
    val estimatedCount: Int,
    val widthMm: Int,
    val heightMm: Int
)
