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
    val variants: List<TileVariantDto>,
    val layouts: List<TileLayoutDto> = emptyList(),
    val websiteUrl: String? = null,
    val usageWays: List<String> = emptyList(),
    val features: List<String> = emptyList(),
    val concreteClass: String? = null,
    val frostResistance: String? = null,
    val waterAbsorptionPercent: String? = null,
    val abrasionClass: String? = null
)

@Serializable
data class TileColorDto(
    val id: Long,
    val name: String,
    val textureUrl: String,
    val hexCode: String,
    val swatchUrl: String? = null,
    val displayOrder: Int = 0
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
    val tilesPerBox: Int? = null,
    val elementSizes: List<TileElementSizeDto> = emptyList(),
    val concreteClass: String? = null,
    val frostResistance: String? = null,
    val waterAbsorptionPercent: String? = null,
    val abrasionClass: String? = null,
    val weightKgPerM2: Double? = null,
    val m2PerPallet: Double? = null
)

@Serializable
data class TileElementSizeDto(
    val widthMm: Int,
    val heightMm: Int,
    val label: String? = null,
    val quantityInPattern: Int? = null
)
