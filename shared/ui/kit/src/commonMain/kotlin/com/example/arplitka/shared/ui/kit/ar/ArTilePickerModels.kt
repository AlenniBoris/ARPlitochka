package com.example.arplitka.shared.ui.kit.ar

import com.example.arplitka.shared.tiles.domain.model.TileUnit

import com.example.arplitka.shared.ui.core.model.ExceptionModelUi

data class ArTileListItemUi(
    val id: Long,
    val name: String,
    val imageUrl: String,
    val basePrice: Double? = null,
    val unit: TileUnit? = null
)

data class ArPickerOptionUi(
    val id: String,
    val name: String,
    val previewUrl: String? = null
)

data class ArPickerPaletteUi(
    val id: String,
    val name: String,
    val swatchUrl: String,
    val isSelected: Boolean = false
)

data class ArTilePickerState(
    val isVisible: Boolean = false,
    val tiles: List<ArTileListItemUi> = emptyList(),
    val selectedTileId: Long? = null,
    val layouts: List<ArPickerOptionUi> = emptyList(),
    val selectedLayoutId: String? = null,
    val palettes: List<ArPickerPaletteUi> = emptyList(),
    val selectedPaletteId: String? = null,
    val isCatalogLoading: Boolean = false,
    val catalogLoadError: ExceptionModelUi? = null
)

data class ArTileSelectionUi(
    val tileId: Long,
    val tileName: String,
    val layoutId: String,
    val paletteId: String,
    val textureUrl: String
)
