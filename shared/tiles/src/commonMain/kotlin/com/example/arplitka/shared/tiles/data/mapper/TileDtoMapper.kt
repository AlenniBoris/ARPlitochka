package com.example.arplitka.shared.tiles.data.mapper

import com.example.arplitka.shared.tiles.data.remote.dto.TileColorDto
import com.example.arplitka.shared.tiles.data.remote.dto.TileDto
import com.example.arplitka.shared.tiles.data.remote.dto.TileElementSizeDto
import com.example.arplitka.shared.tiles.data.remote.dto.TileVariantDto
import com.example.arplitka.shared.tiles.domain.model.Tile
import com.example.arplitka.shared.tiles.domain.model.TileColor
import com.example.arplitka.shared.tiles.domain.model.TileElementSize
import com.example.arplitka.shared.tiles.domain.model.TileFeature
import com.example.arplitka.shared.tiles.domain.model.TileUnit
import com.example.arplitka.shared.tiles.domain.model.TileUsageWay
import com.example.arplitka.shared.tiles.domain.model.TileVariant

fun TileDto.toDomain(): Tile {
    val firstVariant = variants.firstOrNull()
    return Tile(
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
        variants = variants.map(TileVariantDto::toDomain),
        layouts = resolveLayouts(),
        websiteUrl = websiteUrl,
        usageWays = usageWays.mapNotNull { it.toTileUsageWay() },
        features = features.mapNotNull { it.toTileFeature() },
        concreteClass = concreteClass ?: firstVariant?.concreteClass,
        frostResistance = frostResistance ?: firstVariant?.frostResistance,
        waterAbsorptionPercent = waterAbsorptionPercent ?: firstVariant?.waterAbsorptionPercent,
        abrasionClass = abrasionClass ?: firstVariant?.abrasionClass
    )
}

private fun TileColorDto.toDomain(): TileColor = TileColor(
    id = id,
    name = name,
    textureUrl = textureUrl,
    hexCode = hexCode,
    swatchUrl = swatchUrl ?: textureUrl,
    displayOrder = displayOrder
)

private fun TileVariantDto.toDomain(): TileVariant = TileVariant(
    id = id,
    colorId = colorId,
    widthMm = widthMm,
    heightMm = heightMm,
    thicknessMm = thicknessMm,
    price = price,
    stockCount = stockCount,
    tilesPerBox = tilesPerBox,
    elementSizes = elementSizes.map(TileElementSizeDto::toDomain),
    weightKgPerM2 = weightKgPerM2,
    m2PerPallet = m2PerPallet
)

private fun TileElementSizeDto.toDomain(): TileElementSize = TileElementSize(
    widthMm = widthMm,
    heightMm = heightMm,
    label = label,
    quantityInPattern = quantityInPattern
)

private fun String.toTileUnit(): TileUnit = when (lowercase()) {
    "m2" -> TileUnit.M2
    "piece" -> TileUnit.PIECE
    "box" -> TileUnit.BOX
    else -> TileUnit.M2
}

private fun String.toTileUsageWay(): TileUsageWay? = when (uppercase()) {
    "HOME_AND_GARDEN" -> TileUsageWay.HOME_AND_GARDEN
    "PUBLIC_SPACE" -> TileUsageWay.PUBLIC_SPACE
    "DRIVEWAY" -> TileUsageWay.DRIVEWAY
    "PEDESTRIAN_AREA" -> TileUsageWay.PEDESTRIAN_AREA
    else -> null
}

private fun String.toTileFeature(): TileFeature? = when (uppercase()) {
    "MICRO_BEVEL" -> TileFeature.MICRO_BEVEL
    "BEVEL" -> TileFeature.BEVEL
    "ANTI_SLIP" -> TileFeature.ANTI_SLIP
    "FROST_RESISTANT" -> TileFeature.FROST_RESISTANT
    "COLOR_MIX" -> TileFeature.COLOR_MIX
    "TEXTURED_SURFACE" -> TileFeature.TEXTURED_SURFACE
    else -> null
}
