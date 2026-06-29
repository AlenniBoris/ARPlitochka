package com.example.arplitka.shared.ui.kit.ar

import com.example.arplitka.shared.tiles.domain.model.Tile
import com.example.arplitka.shared.tiles.domain.model.TileLayout
import com.example.arplitka.shared.tiles.domain.model.TilePalette
import com.example.arplitka.shared.tiles.domain.model.TileSelection
import com.example.arplitka.shared.ui.core.model.ExceptionModelUi

fun Tile.toArTileListItem(): ArTileListItemUi =
    ArTileListItemUi(
        id = id,
        name = name,
        imageUrl = photos.firstOrNull().orEmpty(),
        basePrice = basePrice,
        unit = unit
    )

fun TileLayout.toPickerOption(): ArPickerOptionUi =
    ArPickerOptionUi(
        id = id,
        name = name,
        previewUrl = previewUrl
    )

fun TilePalette.toPickerPalette(selectedPaletteId: String?): ArPickerPaletteUi =
    ArPickerPaletteUi(
        id = id,
        name = name,
        swatchUrl = previewUrl ?: textureUrl,
        isSelected = id == selectedPaletteId
    )

fun buildArTilePickerState(
    tiles: List<Tile>,
    selectedTile: Tile?,
    selection: TileSelection?,
    isVisible: Boolean,
    isCatalogLoading: Boolean = false,
    catalogLoadError: ExceptionModelUi? = null
): ArTilePickerState {
    val selectedTileId = selection?.tileId
    val tile = selectedTile?.takeIf { it.id == selectedTileId } ?: tiles.find { it.id == selectedTileId }
    val layoutId = selection?.layoutId?.takeIf { it.isNotBlank() }
        ?: tile?.layouts?.firstOrNull()?.id
    val layout = tile?.layouts?.find { it.id == layoutId } ?: tile?.layouts?.firstOrNull()
    val paletteId = selection?.paletteId?.takeIf { it.isNotBlank() }
        ?: layout?.palettes?.firstOrNull()?.id

    return ArTilePickerState(
        isVisible = isVisible,
        tiles = tiles.map { it.toArTileListItem() },
        selectedTileId = selectedTileId,
        layouts = tile?.layouts.orEmpty().map { it.toPickerOption() },
        selectedLayoutId = layout?.id,
        palettes = layout?.palettes.orEmpty().map { it.toPickerPalette(paletteId) },
        selectedPaletteId = paletteId,
        isCatalogLoading = isCatalogLoading,
        catalogLoadError = catalogLoadError
    )
}

fun buildArColorRailPalettes(tile: Tile?, selection: TileSelection?): List<ArPickerPaletteUi> {
    if (tile == null || selection == null) return emptyList()
    val layout = tile.layouts.find { it.id == selection.layoutId } ?: tile.layouts.firstOrNull()
    return layout?.palettes.orEmpty().map { it.toPickerPalette(selection.paletteId) }
}
