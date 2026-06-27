package com.example.arplitka.shared.tiles.domain.usecase

import com.example.arplitka.shared.tiles.domain.model.Tile
import com.example.arplitka.shared.tiles.domain.model.TileColor
import com.example.arplitka.shared.tiles.domain.model.TileLayout
import com.example.arplitka.shared.tiles.domain.model.TilePalette
import com.example.arplitka.shared.tiles.domain.model.TileSelection
import com.example.arplitka.shared.tiles.domain.model.TileUnit
import com.example.arplitka.shared.tiles.domain.model.TileVariant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BuildArTileTextureUseCaseTest {

    private val useCase = BuildArTileTextureUseCase()

    private val tile = Tile(
        id = 1,
        name = "Test",
        description = "",
        manufacturer = "M",
        category = "paving_stones",
        unit = TileUnit.M2,
        material = "Concrete",
        surfaceType = "Rough",
        basePrice = 100.0,
        photos = emptyList(),
        colors = listOf(TileColor(1, "Grey", "fallback.png", "#808080")),
        variants = listOf(TileVariant(101, 1, 260, 260, 60, 100.0, 10)),
        layouts = listOf(
            TileLayout(
                id = "default_mix",
                name = "Mix",
                defaultTextureUrl = "default.png",
                repeatWidthMm = 949,
                repeatHeightMm = 632,
                palettes = listOf(
                    TilePalette(
                        id = "gray_brown_mix",
                        name = "Gray mix",
                        textureUrl = "palette.png"
                    )
                )
            )
        )
    )

    @Test
    fun `build returns texture from selected palette and layout repeat size`() {
        val selection = TileSelection(
            tileId = 1,
            layoutId = "default_mix",
            paletteId = "gray_brown_mix"
        )

        val texture = useCase.build(tile, selection)

        assertNotNull(texture)
        assertEquals("palette.png", texture.textureUrl)
        assertEquals(949, texture.repeatWidthMm)
        assertEquals(632, texture.repeatLengthMm)
    }
}
