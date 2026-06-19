package com.example.arplitka.mock.tiles

import com.example.arplitka.mock.core.JsonAsset
import com.example.arplitka.mock.core.MockRouteRegistry

fun initTilesMocks() {
    MockRouteRegistry {
        "/tiles" reply JsonAsset("mock/tiles/catalog/all.json")
    }
}

object TileRoutes {
    const val GET_TILES = "/tiles"
}
