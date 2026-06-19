package com.example.arplitka.shared.app.catalog

import com.example.arplitka.shared.tiles.domain.model.Tile
import com.example.arplitka.shared.tiles.domain.model.TileColor
import com.example.arplitka.shared.tiles.domain.model.TileUnit
import com.example.arplitka.shared.tiles.domain.model.TileVariant

object SampleTileCatalog {
    val tiles = listOf(
        Tile(
            id = 1L,
            name = "Classic Paving Stone",
            description = "Классическая тротуарная плитка для двора, террасы и дорожек.",
            manufacturer = "Plitka Pro",
            category = "paving_stones",
            unit = TileUnit.M2,
            material = "Concrete",
            surfaceType = "Rough",
            basePrice = 95.0,
            photos = listOf("mock://tiles/previews/paving_stones_v1_preview.png"),
            colors = listOf(
                TileColor(
                    id = 1L,
                    name = "Серый",
                    textureUrl = "mock://tiles/textures/paving_stones_v1.png",
                    hexCode = "#808080"
                ),
                TileColor(
                    id = 2L,
                    name = "Песочный",
                    textureUrl = "mock://tiles/textures/paving_stones_v2.png",
                    hexCode = "#C2B280"
                )
            ),
            variants = listOf(
                TileVariant(
                    id = 101L,
                    colorId = 1L,
                    widthMm = 260,
                    heightMm = 260,
                    thicknessMm = 60,
                    price = 95.0,
                    stockCount = 150,
                    tilesPerBox = 10
                ),
                TileVariant(
                    id = 102L,
                    colorId = 1L,
                    widthMm = 520,
                    heightMm = 260,
                    thicknessMm = 60,
                    price = 110.0,
                    stockCount = 80,
                    tilesPerBox = 5
                )
            )
        )
    )
}
