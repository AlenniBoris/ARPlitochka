package com.example.arplitka.shared.tiles.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TilesResponseDto(
    val items: List<TileDto>
)

@Serializable
data class TileDto(
    val id: Long,
    val name: String,
    val description: String,
    val manufacturer: String,
    val category: String,
    val unit: String,
    val material: String,
    val surfaceType: String,
    val basePrice: Double,
    val photos: List<String>,
    val colors: List<TileColorDto>,
    val variants: List<TileVariantDto>
)

@Serializable
data class TileColorDto(
    val id: Long,
    val name: String,
    val textureUrl: String,
    val hexCode: String
)

@Serializable
data class TileVariantDto(
    val id: Long,
    val colorId: Long,
    val widthMm: Int,
    val heightMm: Int,
    val thicknessMm: Int,
    val price: Double,
    val stockCount: Int,
    val tilesPerBox: Int? = null
)
