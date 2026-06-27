package com.example.arplitka.shared.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object CatalogRoute

@Serializable
data class ArRoute(
    val tileId: Long? = null,
    val layoutId: String? = null,
    val paletteId: String? = null
)

@Serializable
data class TileDetailsRoute(val tileId: Long)

@Serializable
data class TransitionToCatalogRoute(val dummy: Int = 0)

@Serializable
data class TransitionToArRoute(
    val tileId: Long? = null,
    val layoutId: String? = null,
    val paletteId: String? = null
)
