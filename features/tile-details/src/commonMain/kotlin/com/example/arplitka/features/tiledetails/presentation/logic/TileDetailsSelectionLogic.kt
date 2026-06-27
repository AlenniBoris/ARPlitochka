package com.example.arplitka.features.tiledetails.presentation.logic

import com.example.arplitka.features.tiledetails.presentation.model.TileColorOptionUi
import com.example.arplitka.features.tiledetails.presentation.model.TileLayoutOptionUi
import com.example.arplitka.features.tiledetails.presentation.model.TilePaletteOptionUi
import com.example.arplitka.features.tiledetails.presentation.model.TileThicknessOptionUi
import com.example.arplitka.shared.tiles.domain.model.Tile
import com.example.arplitka.shared.tiles.domain.model.TileLayout
import com.example.arplitka.shared.tiles.domain.model.TilePalette
import com.example.arplitka.shared.tiles.domain.model.TileSelection
import com.example.arplitka.shared.tiles.domain.model.TileVariant

object TileDetailsSelectionLogic {

    fun initialLayoutId(tile: Tile): String =
        tile.layouts.firstOrNull()?.id ?: "default"

    fun initialPaletteId(tile: Tile, layoutId: String): String {
        val layout = findLayout(tile, layoutId)
        return layout?.palettes?.firstOrNull()?.id ?: ""
    }

    fun initialColorsBySlot(layout: TileLayout?, palette: TilePalette?): Map<String, Long> =
        palette?.selectedColorsBySlot.orEmpty()

    fun initialThicknessMm(tile: Tile, colorId: Long): Int =
        availableThicknesses(tile, colorId).firstOrNull()
            ?: tile.variants.firstOrNull()?.thicknessMm
            ?: 0

    fun initialColorId(tile: Tile): Long =
        tile.colors.sortedBy { it.displayOrder }.firstOrNull()?.id
            ?: tile.colors.firstOrNull()?.id
            ?: 0L

    fun findLayout(tile: Tile, layoutId: String): TileLayout? =
        tile.layouts.find { it.id == layoutId } ?: tile.layouts.firstOrNull()

    fun findPalette(layout: TileLayout?, paletteId: String): TilePalette? =
        layout?.palettes?.find { it.id == paletteId } ?: layout?.palettes?.firstOrNull()

    fun dominantColorId(palette: TilePalette?): Long =
        palette?.selectedColorsBySlot?.values?.firstOrNull() ?: 0L

    fun availableThicknesses(tile: Tile, colorId: Long): List<Int> =
        tile.variants
            .filter { it.colorId == colorId }
            .map { it.thicknessMm }
            .distinct()
            .sorted()

    fun resolveVariant(tile: Tile, colorId: Long, thicknessMm: Int): TileVariant? =
        tile.variants.find { it.colorId == colorId && it.thicknessMm == thicknessMm }
            ?: tile.variants.find { it.colorId == colorId }
            ?: tile.variants.find { it.thicknessMm == thicknessMm }
            ?: tile.variants.firstOrNull()

    fun buildLayoutOptions(tile: Tile, selectedLayoutId: String): List<TileLayoutOptionUi> =
        tile.layouts.map { layout ->
            TileLayoutOptionUi(
                id = layout.id,
                name = layout.name,
                previewUrl = layout.previewUrl ?: layout.defaultTextureUrl,
                isSelected = layout.id == selectedLayoutId
            )
        }

    fun buildPaletteOptions(
        layout: TileLayout?,
        selectedPaletteId: String
    ): List<TilePaletteOptionUi> =
        layout?.palettes?.map { palette ->
            TilePaletteOptionUi(
                id = palette.id,
                name = palette.name,
                swatchUrl = palette.previewUrl ?: palette.textureUrl,
                hexCode = "#808080",
                isSelected = palette.id == selectedPaletteId
            )
        } ?: emptyList()

    fun buildColorOptions(tile: Tile, selectedColorId: Long): List<TileColorOptionUi> =
        tile.colors.sortedBy { it.displayOrder }.map { color ->
            TileColorOptionUi(
                id = color.id,
                name = color.name,
                swatchUrl = color.swatchUrl ?: color.textureUrl,
                hexCode = color.hexCode,
                isSelected = color.id == selectedColorId
            )
        }

    fun buildThicknessOptions(
        tile: Tile,
        colorId: Long,
        selectedThicknessMm: Int
    ): List<TileThicknessOptionUi> =
        availableThicknesses(tile, colorId).map { thicknessMm ->
            TileThicknessOptionUi(
                thicknessMm = thicknessMm,
                label = "$thicknessMm мм",
                isSelected = thicknessMm == selectedThicknessMm
            )
        }

    fun buildTileSelection(
        tile: Tile,
        layoutId: String,
        paletteId: String,
        selectedColorsBySlot: Map<String, Long>,
        thicknessMm: Int
    ): TileSelection {
        val colorId = dominantColorId(findPalette(findLayout(tile, layoutId), paletteId))
        val variant = resolveVariant(tile, colorId, thicknessMm)

        return TileSelection(
            tileId = tile.id,
            layoutId = layoutId,
            paletteId = paletteId,
            selectedColorsBySlot = selectedColorsBySlot,
            variantId = variant?.id,
            thicknessMm = thicknessMm
        )
    }

    fun selectedTextureUrl(layout: TileLayout?, palette: TilePalette?): String =
        palette?.textureUrl ?: layout?.defaultTextureUrl.orEmpty()
}
