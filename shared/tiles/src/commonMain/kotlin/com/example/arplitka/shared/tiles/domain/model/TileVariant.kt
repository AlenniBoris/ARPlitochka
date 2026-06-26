package com.example.arplitka.shared.tiles.domain.model

data class TileVariant(
    val id: Long,
    val colorId: Long,
    val widthMm: Int,
    val heightMm: Int,
    val thicknessMm: Int,
    val price: Double,
    val stockCount: Int,
    val tilesPerBox: Int? = null,
    val elementSizes: List<TileElementSize> = emptyList(),
    val weightKgPerM2: Double? = null,
    val m2PerPallet: Double? = null
)
