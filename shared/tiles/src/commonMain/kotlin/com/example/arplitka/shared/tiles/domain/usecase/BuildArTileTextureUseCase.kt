package com.example.arplitka.shared.tiles.domain.usecase

import com.example.arplitka.shared.ar.contracts.model.ArTileTexture
import com.example.arplitka.shared.tiles.domain.model.Tile
import com.example.arplitka.shared.tiles.domain.model.TileLayout
import com.example.arplitka.shared.tiles.domain.model.TilePalette
import com.example.arplitka.shared.tiles.domain.model.TileSelection

class BuildArTileTextureUseCase {

    fun build(tile: Tile, selection: TileSelection, rotationDegrees: Float = 0f): ArTileTexture? {
        val layout = tile.layouts.find { it.id == selection.layoutId } ?: tile.layouts.firstOrNull()
        val palette = layout?.palettes?.find { it.id == selection.paletteId }
            ?: layout?.palettes?.firstOrNull()

        if (layout == null || palette == null) {
            return null
        }

        return ArTileTexture(
            textureUrl = palette.textureUrl,
            repeatWidthMm = layout.repeatWidthMm,
            repeatLengthMm = layout.repeatHeightMm,
            rotationDegrees = rotationDegrees
        )
    }

    fun buildFromLayout(layout: TileLayout, palette: TilePalette, rotationDegrees: Float = 0f): ArTileTexture =
        ArTileTexture(
            textureUrl = palette.textureUrl,
            repeatWidthMm = layout.repeatWidthMm,
            repeatLengthMm = layout.repeatHeightMm,
            rotationDegrees = rotationDegrees
        )
}
