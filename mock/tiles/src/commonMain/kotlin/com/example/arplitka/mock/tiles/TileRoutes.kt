package com.example.arplitka.mock.tiles

import com.example.arplitka.mock.core.MockRoute
import com.example.arplitka.mock.core.dsl.GetRoutes

val GetRoutes.tiles: TileGetRoutes
    get() = TileGetRoutes

object TileGetRoutes {
    val all = MockRoute(
        method = "GET",
        path = "/api/v1/tile-collections"
    )
}
