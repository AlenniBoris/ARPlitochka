package com.example.arplitka.shared.ui.navigation

import com.example.arplitka.shared.tiles.domain.model.TileSelection
import kotlinx.serialization.Serializable

@Serializable
data class ArNavigationArgs(
    val tileId: Long? = null,
    val layoutId: String? = null,
    val paletteId: String? = null
) {
    companion object {
        val Empty = ArNavigationArgs()
    }
}

fun TileSelection.toArNavigationArgs(): ArNavigationArgs = ArNavigationArgs(
    tileId = tileId,
    layoutId = layoutId,
    paletteId = paletteId
)

fun ArNavigationArgs.hasInitialTile(): Boolean = tileId != null
