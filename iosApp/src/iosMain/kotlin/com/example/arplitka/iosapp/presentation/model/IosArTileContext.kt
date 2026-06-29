package com.example.arplitka.iosapp.presentation.model

import com.example.arplitka.shared.ar.contracts.model.ArTileTexture
import com.example.arplitka.shared.core.domain.model.CommonException
import com.example.arplitka.shared.core.domain.model.CustomResultModelDomain
import com.example.arplitka.shared.tiles.domain.logic.ArTileSelectionResolver
import com.example.arplitka.shared.tiles.domain.model.ArCatalogState
import com.example.arplitka.shared.tiles.domain.model.Tile
import com.example.arplitka.shared.tiles.domain.model.TileSelection
import com.example.arplitka.shared.tiles.domain.usecase.BuildArTileTextureUseCase
import com.example.arplitka.shared.tiles.domain.usecase.GetTilesUseCase
import com.example.arplitka.shared.ui.core.model.toUiModel
import com.example.arplitka.shared.ui.kit.ar.ArPickerPaletteUi
import com.example.arplitka.shared.ui.kit.ar.ArTilePickerState
import com.example.arplitka.shared.ui.kit.ar.buildArColorRailPalettes
import com.example.arplitka.shared.ui.kit.ar.buildArTilePickerState

internal class IosArTileContext(
    private val getTilesUseCase: GetTilesUseCase,
    private val buildArTileTextureUseCase: BuildArTileTextureUseCase,
    private val onError: (CommonException) -> Unit
) {
    var selectedTile: Tile? = null
    var tileSelection: TileSelection? = null
    var arTileTexture: ArTileTexture? = null
    var selectedTileName: String? = null
    var colorRailPalettes: List<ArPickerPaletteUi> = emptyList()
    var tilePicker: ArTilePickerState = ArTilePickerState()
    var pendingAutoApply: Boolean = false
    var catalogState: ArCatalogState = ArCatalogState.Initial

    private var initialTileId: Long? = null
    private var initialLayoutId: String? = null
    private var initialPaletteId: String? = null
    private var initialSelectionHandled = false

    fun setInitialSelection(tileId: Long?, layoutId: String?, paletteId: String?) {
        initialTileId = tileId
        initialLayoutId = layoutId
        initialPaletteId = paletteId
        initialSelectionHandled = false
    }

    suspend fun loadCatalog() {
        val current = catalogState
        if (current is ArCatalogState.Content) {
            refreshPicker()
            return
        }
        if (current is ArCatalogState.Loading) return

        catalogState = ArCatalogState.Loading
        refreshPicker()

        when (val result = getTilesUseCase()) {
            is CustomResultModelDomain.Success -> {
                val tiles = result.result
                catalogState = ArCatalogState.Content(tiles)
                tryApplyInitialSelection(tiles)
                refreshPicker()
            }
            is CustomResultModelDomain.Error -> {
                catalogState = ArCatalogState.Error(result.exception)
                refreshPicker()
            }
        }
    }

    suspend fun retryCatalogLoad() {
        catalogState = ArCatalogState.Loading
        refreshPicker()

        when (val result = getTilesUseCase()) {
            is CustomResultModelDomain.Success -> {
                val tiles = result.result
                catalogState = ArCatalogState.Content(tiles)
                tryApplyInitialSelection(tiles)
            }
            is CustomResultModelDomain.Error -> {
                catalogState = ArCatalogState.Error(result.exception)
            }
        }
        refreshPicker()
    }

    private fun tryApplyInitialSelection(tiles: List<Tile>) {
        if (initialSelectionHandled) return
        initialSelectionHandled = true
        val tileId = initialTileId ?: return

        val tile = tiles.find { it.id == tileId }
        if (tile == null) {
            onError(CommonException.Client)
            return
        }
        applySelectedTile(
            tile = tile,
            layoutId = initialLayoutId,
            paletteId = initialPaletteId,
            autoApplyOnConfirm = true
        )
    }

    fun openPicker() {
        tilePicker = buildPickerState(isVisible = true)
    }

    fun closePicker() {
        tilePicker = tilePicker.copy(isVisible = false)
    }

    fun onPickerTileSelected(
        tileId: Long,
        isTileVisible: Boolean,
        onDeselect: () -> Unit,
        onApply: () -> Unit
    ) {
        if (tileSelection?.tileId == tileId) {
            onDeselect()
            return
        }

        val tiles = (catalogState as? ArCatalogState.Content)?.tiles
        val tile = tiles?.find { it.id == tileId }
        if (tile == null) {
            onError(CommonException.Client)
            return
        }

        applySelectedTile(
            tile = tile,
            layoutId = null,
            paletteId = null,
            autoApplyOnConfirm = false
        )
        if (pendingAutoApply || isTileVisible) {
            onApply()
            pendingAutoApply = false
        }
        refreshPicker()
    }

    fun clearTileSelection() {
        selectedTile = null
        tileSelection = null
        arTileTexture = null
        selectedTileName = null
        colorRailPalettes = emptyList()
        pendingAutoApply = false
        refreshPicker()
    }

    fun onPickerLayoutSelected(layoutId: String, onApply: () -> Unit) {
        val tile = selectedTile ?: return
        val selection = tileSelection ?: return
        if (selection.layoutId == layoutId) return
        val layout = tile.layouts.find { it.id == layoutId } ?: return
        val palette = layout.palettes.firstOrNull() ?: return
        applySelection(
            tile = tile,
            selection = TileSelection(
                tileId = tile.id,
                layoutId = layout.id,
                paletteId = palette.id,
                selectedColorsBySlot = palette.selectedColorsBySlot
            )
        )
        onApply()
        refreshPicker()
    }

    fun onPickerPaletteSelected(paletteId: String, onApply: () -> Unit) {
        val tile = selectedTile ?: return
        val selection = tileSelection ?: return
        if (selection.paletteId == paletteId) return
        val layout = tile.layouts.find { it.id == selection.layoutId } ?: tile.layouts.firstOrNull() ?: return
        val palette = layout.palettes.find { it.id == paletteId } ?: return
        applySelection(
            tile = tile,
            selection = selection.copy(
                paletteId = palette.id,
                selectedColorsBySlot = palette.selectedColorsBySlot
            )
        )
        onApply()
        refreshPicker()
    }

    private fun applySelectedTile(
        tile: Tile,
        layoutId: String?,
        paletteId: String?,
        autoApplyOnConfirm: Boolean
    ) {
        selectedTile = tile
        val selection = ArTileSelectionResolver.resolveSelection(tile, layoutId, paletteId)
        val texture = ArTileSelectionResolver.buildTexture(
            tile = tile,
            selection = selection,
            rotationDegrees = 0f,
            buildArTileTextureUseCase = buildArTileTextureUseCase
        )

        tileSelection = selection
        arTileTexture = texture
        selectedTileName = tile.name
        colorRailPalettes = buildArColorRailPalettes(tile, selection)
        pendingAutoApply = autoApplyOnConfirm

        if (texture == null) {
            onError(CommonException.Serialization)
        }
    }

    private fun applySelection(tile: Tile, selection: TileSelection) {
        val texture = ArTileSelectionResolver.buildTexture(
            tile = tile,
            selection = selection,
            rotationDegrees = arTileTexture?.rotationDegrees ?: 0f,
            buildArTileTextureUseCase = buildArTileTextureUseCase
        ) ?: return

        selectedTile = tile
        tileSelection = selection
        arTileTexture = texture
        selectedTileName = tile.name
        colorRailPalettes = buildArColorRailPalettes(tile, selection)
    }

    private fun buildPickerState(
        isVisible: Boolean,
        selectedTileOverride: Tile? = selectedTile,
        selectionOverride: TileSelection? = tileSelection
    ): ArTilePickerState {
        return when (val catalog = catalogState) {
            ArCatalogState.Initial, ArCatalogState.Loading -> ArTilePickerState(
                isVisible = isVisible,
                isCatalogLoading = true
            )
            is ArCatalogState.Error -> ArTilePickerState(
                isVisible = isVisible,
                catalogLoadError = catalog.exception.toUiModel()
            )
            is ArCatalogState.Content -> buildArTilePickerState(
                tiles = catalog.tiles,
                selectedTile = selectedTileOverride,
                selection = selectionOverride,
                isVisible = isVisible
            )
        }
    }

    private fun refreshPicker() {
        tilePicker = buildPickerState(isVisible = tilePicker.isVisible)
        colorRailPalettes = buildArColorRailPalettes(selectedTile, tileSelection)
    }
}
