package com.example.arplitka.mock.tiles

import com.example.arplitka.mock.core.JsonAsset

val JsonAsset.tiles: TileAssets
    get() = TileAssets

object TileAssets {
    const val all = "mock/tiles/catalog/all.json"
    const val pavingStonesV1Texture = "mock/tiles/textures/paving_stones_v1.png"
    const val pavingStonesV2Texture = "mock/tiles/textures/paving_stones_v2.png"
    const val pavingStonesV1Preview = "mock/tiles/previews/paving_stones_v1_preview.png"
    const val pavingStonesV2Preview = "mock/tiles/previews/paving_stones_v2_preview.png"
}
