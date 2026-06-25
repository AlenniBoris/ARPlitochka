package com.example.arplitka.mock.tiles

import com.example.arplitka.mock.core.JsonAsset
import com.example.arplitka.mock.core.MockRouteRegistry

fun initTilesMocks() {
    MockRouteRegistry {
        TileRoutes.TILES reply JsonAsset("mock/tiles/catalog/all.json")
        
        // Регистрируем моки для каждой плитки по ID
        TileRoutes.tileById(1) reply JsonAsset("mock/tiles/details/tile_1.json")
        TileRoutes.tileById(2) reply JsonAsset("mock/tiles/details/tile_2.json")
        TileRoutes.tileById(3) reply JsonAsset("mock/tiles/details/tile_3.json")
        TileRoutes.tileById(4) reply JsonAsset("mock/tiles/details/tile_4.json")
        TileRoutes.tileById(5) reply JsonAsset("mock/tiles/details/tile_5.json")
        TileRoutes.tileById(6) reply JsonAsset("mock/tiles/details/tile_6.json")
        TileRoutes.tileById(7) reply JsonAsset("mock/tiles/details/tile_7.json")
        TileRoutes.tileById(8) reply JsonAsset("mock/tiles/details/tile_8.json")
        TileRoutes.tileById(9) reply JsonAsset("mock/tiles/details/tile_9.json")
        TileRoutes.tileById(10) reply JsonAsset("mock/tiles/details/tile_10.json")
        TileRoutes.tileById(11) reply JsonAsset("mock/tiles/details/tile_11.json")
    }
}

object TileRoutes {
    const val TILES = "/tiles"
    const val TILE_DETAILS = "/tiles/"
    
    fun tileById(id: Long): String = TILE_DETAILS + id.toString()
}
