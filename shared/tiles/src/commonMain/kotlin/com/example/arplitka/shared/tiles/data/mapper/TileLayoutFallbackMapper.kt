package com.example.arplitka.shared.tiles.data.mapper

import com.example.arplitka.shared.tiles.data.remote.dto.TileDto
import com.example.arplitka.shared.tiles.data.remote.dto.TileElementColorOptionDto
import com.example.arplitka.shared.tiles.data.remote.dto.TileLayoutDto
import com.example.arplitka.shared.tiles.data.remote.dto.TileLayoutElementDto
import com.example.arplitka.shared.tiles.data.remote.dto.TilePaletteDto
import com.example.arplitka.shared.tiles.domain.model.TileElementColorOption
import com.example.arplitka.shared.tiles.domain.model.TileLayout
import com.example.arplitka.shared.tiles.domain.model.TileLayoutElement
import com.example.arplitka.shared.tiles.domain.model.TilePalette

internal fun TileDto.resolveLayouts(): List<TileLayout> {
    if (layouts.isNotEmpty()) {
        return layouts.map { it.toDomain() }
    }
    return buildFallbackLayouts()
}

private fun TileDto.buildFallbackLayouts(): List<TileLayout> {
    val firstVariant = variants.firstOrNull()
    val repeatWidthMm = firstVariant?.widthMm ?: 260
    val repeatHeightMm = firstVariant?.heightMm ?: 260
    val defaultTextureUrl = colors.firstOrNull()?.textureUrl.orEmpty()

    val elementSizes = firstVariant?.elementSizes.orEmpty()
    val elements = if (elementSizes.isNotEmpty()) {
        elementSizes.mapIndexed { index, size ->
            val slotId = "slot_$index"
            TileLayoutElement(
                elementTypeId = "element_${size.widthMm}x${size.heightMm}",
                name = size.label ?: "${size.widthMm}x${size.heightMm}",
                widthMm = size.widthMm,
                heightMm = size.heightMm,
                countInRepeat = size.quantityInPattern ?: 1,
                colorSlotId = slotId,
                colorOptions = colors.map { color ->
                    TileElementColorOption(
                        colorId = color.id,
                        name = color.name,
                        hexCode = color.hexCode,
                        textureUrl = color.textureUrl,
                        swatchUrl = color.swatchUrl ?: color.textureUrl,
                        sku = null
                    )
                }
            )
        }
    } else {
        listOf(
            TileLayoutElement(
                elementTypeId = "element_${repeatWidthMm}x$repeatHeightMm",
                name = "${repeatWidthMm}x$repeatHeightMm",
                widthMm = repeatWidthMm,
                heightMm = repeatHeightMm,
                countInRepeat = 1,
                colorSlotId = "default",
                colorOptions = colors.map { color ->
                    TileElementColorOption(
                        colorId = color.id,
                        name = color.name,
                        hexCode = color.hexCode,
                        textureUrl = color.textureUrl,
                        swatchUrl = color.swatchUrl ?: color.textureUrl,
                        sku = null
                    )
                }
            )
        )
    }

    val palettes = colors.map { color ->
        TilePalette(
            id = "color_${color.id}",
            name = color.name,
            textureUrl = color.textureUrl,
            previewUrl = color.swatchUrl ?: color.textureUrl,
            selectedColorsBySlot = elements.associate { element ->
                element.colorSlotId to color.id
            }
        )
    }

    return listOf(
        TileLayout(
            id = "default",
            name = "Стандарт",
            previewUrl = colors.firstOrNull()?.swatchUrl ?: colors.firstOrNull()?.textureUrl,
            defaultTextureUrl = defaultTextureUrl,
            repeatWidthMm = repeatWidthMm,
            repeatHeightMm = repeatHeightMm,
            elements = elements,
            palettes = palettes
        )
    )
}

private fun TileLayoutDto.toDomain(): TileLayout = TileLayout(
    id = id,
    name = name,
    previewUrl = previewUrl,
    defaultTextureUrl = defaultTextureUrl,
    repeatWidthMm = repeatWidthMm,
    repeatHeightMm = repeatHeightMm,
    elements = elements.map(TileLayoutElementDto::toDomain),
    palettes = palettes.map(TilePaletteDto::toDomain)
)

private fun TileLayoutElementDto.toDomain(): TileLayoutElement = TileLayoutElement(
    elementTypeId = elementTypeId,
    name = name,
    widthMm = widthMm,
    heightMm = heightMm,
    countInRepeat = countInRepeat,
    colorSlotId = colorSlotId,
    colorOptions = colorOptions.map(TileElementColorOptionDto::toDomain)
)

private fun TileElementColorOptionDto.toDomain(): TileElementColorOption = TileElementColorOption(
    colorId = colorId,
    name = name,
    hexCode = hexCode,
    textureUrl = textureUrl,
    swatchUrl = swatchUrl,
    sku = sku
)

private fun TilePaletteDto.toDomain(): TilePalette = TilePalette(
    id = id,
    name = name,
    textureUrl = textureUrl,
    previewUrl = previewUrl,
    selectedColorsBySlot = selectedColorsBySlot
)
