package com.example.arplitka.shared.tiles.data.mapper

import com.example.arplitka.shared.tiles.data.remote.dto.TileColorDto
import com.example.arplitka.shared.tiles.data.remote.dto.TileDto
import com.example.arplitka.shared.tiles.data.remote.dto.TileVariantDto
import com.example.arplitka.shared.tiles.domain.model.Tile
import com.example.arplitka.shared.tiles.domain.model.TileColor
import com.example.arplitka.shared.tiles.domain.model.TileUnit
import com.example.arplitka.shared.tiles.domain.model.TileVariant

fun TileDto.toDomain(): Tile = Tile(
    id = id,
    name = name,
    description = description,
    manufacturer = manufacturer,
    category = category,
    unit = unit.toTileUnit(),
    material = material,
    surfaceType = surfaceType,
    basePrice = basePrice,
    photos = photos,
    colors = colors.map(TileColorDto::toDomain),
    variants = variants.map(TileVariantDto::toDomain)
)

private fun TileColorDto.toDomain(): TileColor = TileColor(
    id = id,
    name = name,
    textureUrl = textureUrl,
    hexCode = hexCode
)

private fun TileVariantDto.toDomain(): TileVariant = TileVariant(
    id = id,
    colorId = colorId,
    widthMm = widthMm,
    heightMm = heightMm,
    thicknessMm = thicknessMm,
    price = price,
    stockCount = stockCount,
    tilesPerBox = tilesPerBox
)

private fun String.toTileUnit(): TileUnit = when (lowercase()) {
    "m2" -> TileUnit.M2
    "piece" -> TileUnit.PIECE
    "box" -> TileUnit.BOX
    else -> TileUnit.M2
}
