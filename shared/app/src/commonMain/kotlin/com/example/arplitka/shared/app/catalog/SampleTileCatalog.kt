package com.example.arplitka.shared.app.catalog

import com.example.arplitka.shared.tiles.domain.model.RepeatPattern
import com.example.arplitka.shared.tiles.domain.model.TileCollection
import com.example.arplitka.shared.tiles.domain.model.TileManufacturer
import com.example.arplitka.shared.tiles.domain.model.TilePattern
import com.example.arplitka.shared.tiles.domain.model.TilePrice
import com.example.arplitka.shared.tiles.domain.model.TilePriceUnit
import com.example.arplitka.shared.tiles.domain.model.TileStockStatus
import com.example.arplitka.shared.tiles.domain.model.TileTexture
import com.example.arplitka.shared.tiles.domain.model.TileTextureStatus
import com.example.arplitka.shared.tiles.domain.model.TileVariant

object SampleTileCatalog {
    val collections = listOf(
        TileCollection(
            id = 1L,
            slug = "paving-stone-classic",
            name = "Classic Paving Stone",
            description = "Классическая тротуарная плитка для двора, террасы и дорожек.",
            category = "paving_stones",
            manufacturer = TileManufacturer(
                id = 1L,
                slug = "plitka-pro",
                name = "Plitka Pro"
            ),
            previewImageUrl = "mock://tiles/previews/paving_stones_v1_preview.png",
            textures = listOf(
                TileTexture(
                    id = 1L,
                    code = "gray",
                    name = "Серый",
                    textureUrl = "mock://tiles/textures/paving_stones_v1.png",
                    previewImageUrl = "mock://tiles/previews/paving_stones_v1_preview.png",
                    repeatPattern = RepeatPattern(widthMm = 780, lengthMm = 1040),
                    status = TileTextureStatus.ACTIVE
                ),
                TileTexture(
                    id = 2L,
                    code = "sand",
                    name = "Песочный",
                    textureUrl = "mock://tiles/textures/paving_stones_v2.png",
                    previewImageUrl = "mock://tiles/previews/paving_stones_v2_preview.png",
                    repeatPattern = RepeatPattern(widthMm = 780, lengthMm = 1040),
                    status = TileTextureStatus.ACTIVE
                )
            ),
            tileVariants = listOf(
                TileVariant(
                    id = 101L,
                    code = "small_60",
                    name = "Малая 60 мм",
                    widthMm = 260,
                    lengthMm = 260,
                    thicknessMm = 60,
                    stockStatus = TileStockStatus.IN_STOCK,
                    price = TilePrice(amount = 95.0, unit = TilePriceUnit.M2)
                ),
                TileVariant(
                    id = 102L,
                    code = "medium_60",
                    name = "Средняя 60 мм",
                    widthMm = 520,
                    lengthMm = 260,
                    thicknessMm = 60,
                    stockStatus = TileStockStatus.IN_STOCK,
                    price = TilePrice(amount = 110.0, unit = TilePriceUnit.M2)
                )
            ),
            patterns = listOf(
                TilePattern(
                    id = 201L,
                    code = "mixed_small_medium",
                    name = "Комбинированная раскладка",
                    variantIds = listOf(101L, 102L),
                    previewImageUrl = "mock://tiles/previews/paving_stones_v2_preview.png"
                )
            ),
            tags = listOf("outdoor", "frost_resistant", "anti_slip")
        )
    )
}
