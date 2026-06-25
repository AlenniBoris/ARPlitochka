package com.example.arplitka.shared.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object CatalogRoute

@Serializable
object ArRoute

@Serializable
data class TileDetailsRoute(val tileId: Long)

@Serializable
object TransitionToCatalogRoute

@Serializable
object TransitionToArRoute
