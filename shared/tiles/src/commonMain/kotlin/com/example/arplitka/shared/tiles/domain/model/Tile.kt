package com.example.arplitka.shared.tiles.domain.model

data class Tile(
    val id: Long,
    val name: String,
    val description: String,
    val manufacturer: String,
    val category: String,
    val unit: TileUnit,
    val material: String,
    val surfaceType: String,
    val basePrice: Double,
    val photos: List<String>,
    val colors: List<TileColor>,
    val variants: List<TileVariant>,
    val websiteUrl: String? = null,
    val usageWays: List<TileUsageWay> = emptyList(),
    val features: List<TileFeature> = emptyList(),
    val concreteClass: String? = null,
    val frostResistance: String? = null,
    val waterAbsorptionPercent: String? = null,
    val abrasionClass: String? = null
)
