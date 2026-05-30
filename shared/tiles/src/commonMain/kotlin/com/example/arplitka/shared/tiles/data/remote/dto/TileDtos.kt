package com.example.arplitka.shared.tiles.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TileCollectionsResponseDto(
    val items: List<TileCollectionDto>
)

@Serializable
data class TileCollectionDto(
    val id: Long,
    val slug: String,
    val name: String,
    val description: String,
    val category: String,
    val manufacturer: TileManufacturerDto,
    val previewImageUrl: String,
    val textures: List<TileTextureDto>,
    val tileVariants: List<TileVariantDto>,
    val patterns: List<TilePatternDto>,
    val tags: List<String>
)

@Serializable
data class TileManufacturerDto(
    val id: Long,
    val slug: String,
    val name: String
)

@Serializable
data class TileTextureDto(
    val id: Long,
    val code: String,
    val name: String,
    val textureUrl: String,
    val previewImageUrl: String,
    val repeatPattern: RepeatPatternDto,
    val status: String
)

@Serializable
data class RepeatPatternDto(
    val widthMm: Int,
    val lengthMm: Int
)

@Serializable
data class TileVariantDto(
    val id: Long,
    val code: String,
    val name: String,
    val widthMm: Int,
    val lengthMm: Int,
    val thicknessMm: Int,
    val stockStatus: String,
    val price: TilePriceDto
)

@Serializable
data class TilePriceDto(
    val amount: Double,
    val unit: String
)

@Serializable
data class TilePatternDto(
    val id: Long,
    val code: String,
    val name: String,
    val variantIds: List<Long>,
    val previewImageUrl: String
)
