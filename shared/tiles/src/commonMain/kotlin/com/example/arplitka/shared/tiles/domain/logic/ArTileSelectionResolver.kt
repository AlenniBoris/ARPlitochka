package com.example.arplitka.shared.tiles.domain.logic

import com.example.arplitka.shared.ar.contracts.model.ArTileTexture
import com.example.arplitka.shared.tiles.domain.model.Tile
import com.example.arplitka.shared.tiles.domain.model.TileLayout
import com.example.arplitka.shared.tiles.domain.model.TilePalette
import com.example.arplitka.shared.tiles.domain.model.TileSelection
import com.example.arplitka.shared.tiles.domain.usecase.BuildArTileTextureUseCase

object ArTileSelectionResolver {

    fun resolveSelection(
        tile: Tile,
        layoutId: String?,
        paletteId: String?
    ): TileSelection {
        val layout = findLayout(tile, layoutId)
        val palette = findPalette(layout, paletteId)

        return TileSelection(
            tileId = tile.id,
            layoutId = layout?.id.orEmpty(),
            paletteId = palette?.id.orEmpty(),
            selectedColorsBySlot = palette?.selectedColorsBySlot.orEmpty()
        )
    }

    fun buildTexture(
        tile: Tile,
        selection: TileSelection,
        rotationDegrees: Float,
        buildArTileTextureUseCase: BuildArTileTextureUseCase
    ): ArTileTexture? = buildArTileTextureUseCase.build(
        tile = tile,
        selection = selection,
        rotationDegrees = rotationDegrees
    )

    private fun findLayout(tile: Tile, layoutId: String?): TileLayout? =
        layoutId?.let { id -> tile.layouts.find { it.id == id } } ?: tile.layouts.firstOrNull()

    private fun findPalette(layout: TileLayout?, paletteId: String?): TilePalette? =
        paletteId?.let { id -> layout?.palettes?.find { it.id == id } } ?: layout?.palettes?.firstOrNull()
}
