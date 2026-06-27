package com.example.arplitka.features.tiledetails.presentation.viewmodel

import com.example.arplitka.features.tiledetails.presentation.logic.TileDetailsSelectionLogic
import com.example.arplitka.features.tiledetails.presentation.model.TileColorOptionUi
import com.example.arplitka.features.tiledetails.presentation.model.TileLayoutOptionUi
import com.example.arplitka.features.tiledetails.presentation.model.TilePaletteOptionUi
import com.example.arplitka.features.tiledetails.presentation.model.TileThicknessOptionUi
import com.example.arplitka.shared.core.domain.model.CommonException
import com.example.arplitka.shared.tiles.domain.model.Tile
import com.example.arplitka.shared.tiles.domain.model.TileLayout
import com.example.arplitka.shared.tiles.domain.model.TilePalette
import com.example.arplitka.shared.tiles.domain.model.TileSelection
import com.example.arplitka.shared.tiles.domain.model.TileVariant

sealed interface TileDetailsUiState {
    data object Loading : TileDetailsUiState

    data class Content(
        val tile: Tile,
        val selectedLayoutId: String,
        val selectedPaletteId: String,
        val selectedColorsBySlot: Map<String, Long>,
        val selectedThicknessMm: Int,
        val isDescriptionExpanded: Boolean = false
    ) : TileDetailsUiState {

        val layoutOptions: List<TileLayoutOptionUi> =
            TileDetailsSelectionLogic.buildLayoutOptions(tile, selectedLayoutId)

        val selectedLayout: TileLayout? =
            TileDetailsSelectionLogic.findLayout(tile, selectedLayoutId)

        val paletteOptions: List<TilePaletteOptionUi> =
            TileDetailsSelectionLogic.buildPaletteOptions(selectedLayout, selectedPaletteId)

        val selectedPalette: TilePalette? =
            TileDetailsSelectionLogic.findPalette(selectedLayout, selectedPaletteId)

        val showLayoutSelector: Boolean = layoutOptions.size > 1

        val selectedTextureUrl: String =
            TileDetailsSelectionLogic.selectedTextureUrl(selectedLayout, selectedPalette)

        val dominantColorId: Long =
            TileDetailsSelectionLogic.dominantColorId(selectedPalette)

        val colorOptions: List<TileColorOptionUi> =
            paletteOptions.map { palette ->
                TileColorOptionUi(
                    id = palette.id.hashCode().toLong(),
                    name = palette.name,
                    swatchUrl = palette.swatchUrl,
                    hexCode = palette.hexCode,
                    isSelected = palette.isSelected
                )
            }

        val selectedColor: TileColorOptionUi =
            colorOptions.find { it.isSelected }
                ?: colorOptions.firstOrNull()
                ?: TileColorOptionUi(0, "", "", "#000000", true)

        val thicknessOptions: List<TileThicknessOptionUi> =
            TileDetailsSelectionLogic.buildThicknessOptions(tile, dominantColorId, selectedThicknessMm)

        val selectedThickness: TileThicknessOptionUi =
            thicknessOptions.find { it.isSelected }
                ?: thicknessOptions.firstOrNull()
                ?: TileThicknessOptionUi(selectedThicknessMm, "$selectedThicknessMm мм", true)

        val selectedVariant: TileVariant? =
            TileDetailsSelectionLogic.resolveVariant(tile, dominantColorId, selectedThicknessMm)

        val tileSelection: TileSelection =
            TileDetailsSelectionLogic.buildTileSelection(
                tile = tile,
                layoutId = selectedLayoutId,
                paletteId = selectedPaletteId,
                selectedColorsBySlot = selectedColorsBySlot,
                thicknessMm = selectedThicknessMm
            )

        companion object {
            fun initial(tile: Tile): Content {
                val layoutId = TileDetailsSelectionLogic.initialLayoutId(tile)
                val layout = TileDetailsSelectionLogic.findLayout(tile, layoutId)
                val paletteId = TileDetailsSelectionLogic.initialPaletteId(tile, layoutId)
                val palette = TileDetailsSelectionLogic.findPalette(layout, paletteId)
                val colorsBySlot = TileDetailsSelectionLogic.initialColorsBySlot(layout, palette)
                val colorId = TileDetailsSelectionLogic.dominantColorId(palette)
                val thicknessMm = TileDetailsSelectionLogic.initialThicknessMm(tile, colorId)

                return Content(
                    tile = tile,
                    selectedLayoutId = layoutId,
                    selectedPaletteId = paletteId,
                    selectedColorsBySlot = colorsBySlot,
                    selectedThicknessMm = thicknessMm
                )
            }
        }
    }

    data class Error(val exception: CommonException) : TileDetailsUiState
}
