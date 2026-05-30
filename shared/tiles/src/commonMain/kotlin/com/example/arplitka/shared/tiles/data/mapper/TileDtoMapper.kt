package com.example.arplitka.shared.tiles.data.mapper

import com.example.arplitka.shared.tiles.data.remote.dto.RepeatPatternDto
import com.example.arplitka.shared.tiles.data.remote.dto.TileCollectionDto
import com.example.arplitka.shared.tiles.data.remote.dto.TileManufacturerDto
import com.example.arplitka.shared.tiles.data.remote.dto.TilePatternDto
import com.example.arplitka.shared.tiles.data.remote.dto.TilePriceDto
import com.example.arplitka.shared.tiles.data.remote.dto.TileTextureDto
import com.example.arplitka.shared.tiles.data.remote.dto.TileVariantDto
import com.example.arplitka.shared.tiles.domain.model.RepeatPattern
import com.example.arplitka.shared.tiles.domain.model.TileCollection
import com.example.arplitka.shared.tiles.domain.model.TileManufacturer
import com.example.arplitka.shared.tiles.domain.model.TilePattern
import com.example.arplitka.shared.tiles.domain.model.TilePrice
import com.example.arplitka.shared.tiles.domain.model.TilePriceUnit
import com.example.arplitka.shared.tiles.domain.model.TileStockStatus
import com.example.arplitka.shared.tiles.domain.model.TileTexture
import com.example.arplitka.shared.tiles.domain.model.TileTextureStatus
import com.example.arplitka.shared.tiles.domain.model.TileVariant

fun TileCollectionDto.toDomain(): TileCollection = TileCollection(
    id = id,
    slug = slug,
    name = name,
    description = description,
    category = category,
    manufacturer = manufacturer.toDomain(),
    previewImageUrl = previewImageUrl,
    textures = textures.map(TileTextureDto::toDomain),
    tileVariants = tileVariants.map(TileVariantDto::toDomain),
    patterns = patterns.map(TilePatternDto::toDomain),
    tags = tags
)

private fun TileManufacturerDto.toDomain(): TileManufacturer = TileManufacturer(
    id = id,
    slug = slug,
    name = name
)

private fun TileTextureDto.toDomain(): TileTexture = TileTexture(
    id = id,
    code = code,
    name = name,
    textureUrl = textureUrl,
    previewImageUrl = previewImageUrl,
    repeatPattern = repeatPattern.toDomain(),
    status = status.toTextureStatus()
)

private fun RepeatPatternDto.toDomain(): RepeatPattern = RepeatPattern(
    widthMm = widthMm,
    lengthMm = lengthMm
)

private fun TileVariantDto.toDomain(): TileVariant = TileVariant(
    id = id,
    code = code,
    name = name,
    widthMm = widthMm,
    lengthMm = lengthMm,
    thicknessMm = thicknessMm,
    stockStatus = stockStatus.toStockStatus(),
    price = price.toDomain()
)

private fun TilePriceDto.toDomain(): TilePrice = TilePrice(
    amount = amount,
    unit = unit.toPriceUnit()
)

private fun TilePatternDto.toDomain(): TilePattern = TilePattern(
    id = id,
    code = code,
    name = name,
    variantIds = variantIds,
    previewImageUrl = previewImageUrl
)

private fun String.toTextureStatus(): TileTextureStatus = when (lowercase()) {
    "active" -> TileTextureStatus.ACTIVE
    "hidden" -> TileTextureStatus.HIDDEN
    "discontinued" -> TileTextureStatus.DISCONTINUED
    else -> TileTextureStatus.HIDDEN
}

private fun String.toStockStatus(): TileStockStatus = when (lowercase()) {
    "in_stock" -> TileStockStatus.IN_STOCK
    "out_of_stock" -> TileStockStatus.OUT_OF_STOCK
    "preorder" -> TileStockStatus.PREORDER
    "discontinued" -> TileStockStatus.DISCONTINUED
    else -> TileStockStatus.OUT_OF_STOCK
}

private fun String.toPriceUnit(): TilePriceUnit = when (lowercase()) {
    "m2" -> TilePriceUnit.M2
    "piece" -> TilePriceUnit.PIECE
    else -> TilePriceUnit.M2
}
