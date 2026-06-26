package com.example.arplitka.features.tiledetails.presentation.viewmodel

import com.example.arplitka.features.tiledetails.presentation.model.TileColorOptionUi
import com.example.arplitka.features.tiledetails.presentation.model.TileThicknessOptionUi
import com.example.arplitka.shared.core.domain.model.CommonException
import com.example.arplitka.shared.tiles.domain.model.Tile
import com.example.arplitka.shared.tiles.domain.model.TileColor
import com.example.arplitka.shared.tiles.domain.model.TileVariant

sealed interface TileDetailsUiState {
    data object Loading : TileDetailsUiState

    data class Content(
        val tile: Tile,
        val selectedColorId: Long,
        val selectedThicknessMm: Int,
        val isDescriptionExpanded: Boolean = false
    ) : TileDetailsUiState {

        val colorOptions: List<TileColorOptionUi> =
            tile.colors.sortedBy { it.displayOrder }.map { color ->
                TileColorOptionUi(
                    id = color.id,
                    name = color.name,
                    swatchUrl = color.swatchUrl ?: color.textureUrl,
                    hexCode = color.hexCode,
                    isSelected = color.id == selectedColorId
                )
            }

        val thicknessOptions: List<TileThicknessOptionUi> =
            tile.variants
                .filter { it.colorId == selectedColorId }
                .map { it.thicknessMm }
                .distinct()
                .sorted()
                .map { thicknessMm ->
                    TileThicknessOptionUi(
                        thicknessMm = thicknessMm,
                        label = "$thicknessMm мм",
                        isSelected = thicknessMm == selectedThicknessMm
                    )
                }

        val selectedColor: TileColorOptionUi =
            colorOptions.find { it.isSelected }
                ?: colorOptions.firstOrNull()
                ?: TileColorOptionUi(0, "", "", "#000000", true)

        val selectedThickness: TileThicknessOptionUi =
            thicknessOptions.find { it.isSelected }
                ?: thicknessOptions.firstOrNull()
                ?: TileThicknessOptionUi(selectedThicknessMm, "$selectedThicknessMm мм", true)

        val selectedVariant: TileVariant? =
            tile.variants.find { it.colorId == selectedColorId && it.thicknessMm == selectedThicknessMm }
                ?: tile.variants.find { it.colorId == selectedColorId }
                ?: tile.variants.firstOrNull()

        companion object {
            fun initial(tile: Tile): Content {
                val initialColorId = tile.colors.sortedBy { it.displayOrder }.firstOrNull()?.id
                    ?: tile.colors.firstOrNull()?.id
                    ?: 0L

                val initialThicknessMm = tile.variants
                    .filter { it.colorId == initialColorId }
                    .map { it.thicknessMm }
                    .distinct()
                    .sorted()
                    .firstOrNull()
                    ?: tile.variants.firstOrNull()?.thicknessMm
                    ?: 0

                return Content(
                    tile = tile,
                    selectedColorId = initialColorId,
                    selectedThicknessMm = initialThicknessMm
                )
            }
        }
    }

    data class Error(val exception: CommonException) : TileDetailsUiState
}
