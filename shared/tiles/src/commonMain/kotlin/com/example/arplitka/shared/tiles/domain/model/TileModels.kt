package com.example.arplitka.shared.tiles.domain.model

data class TileCollection(
    val id: Long,
    val slug: String,
    val name: String,
    val description: String,
    val category: String,
    val manufacturer: TileManufacturer,
    val previewImageUrl: String,
    val textures: List<TileTexture>,
    val tileVariants: List<TileVariant>,
    val patterns: List<TilePattern>,
    val tags: List<String>
)

data class TileManufacturer(
    val id: Long,
    val slug: String,
    val name: String
)

data class TileTexture(
    val id: Long,
    val code: String,
    val name: String,
    val textureUrl: String,
    val previewImageUrl: String,
    val repeatPattern: RepeatPattern,
    val status: TileTextureStatus
)

data class RepeatPattern(
    val widthMm: Int,
    val lengthMm: Int
)

data class TileVariant(
    val id: Long,
    val code: String,
    val name: String,
    val widthMm: Int,
    val lengthMm: Int,
    val thicknessMm: Int,
    val stockStatus: TileStockStatus,
    val price: TilePrice
)

data class TilePrice(
    val amount: Double,
    val unit: TilePriceUnit
)

data class TilePattern(
    val id: Long,
    val code: String,
    val name: String,
    val variantIds: List<Long>,
    val previewImageUrl: String
)

enum class TileTextureStatus {
    ACTIVE,
    HIDDEN,
    DISCONTINUED
}

enum class TileStockStatus {
    IN_STOCK,
    OUT_OF_STOCK,
    PREORDER,
    DISCONTINUED
}

enum class TilePriceUnit {
    M2,
    PIECE
}
