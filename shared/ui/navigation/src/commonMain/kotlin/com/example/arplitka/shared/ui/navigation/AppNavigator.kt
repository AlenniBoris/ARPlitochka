package com.example.arplitka.shared.ui.navigation

import com.example.arplitka.shared.tiles.domain.model.TileSelection

interface AppNavigator {
    fun openCatalog()
    fun openTile(id: Long)
    fun openAr(selection: TileSelection? = null)
    fun back()
    fun backFromAr(returnToTileId: Long? = null)
}
