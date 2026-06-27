package com.example.arplitka.features.tiledetails.presentation.logic

import com.example.arplitka.shared.tiles.domain.model.Tile
import com.example.arplitka.shared.tiles.domain.model.TileColor
import com.example.arplitka.shared.tiles.domain.model.TileElementColorOption
import com.example.arplitka.shared.tiles.domain.model.TileLayout
import com.example.arplitka.shared.tiles.domain.model.TileLayoutElement
import com.example.arplitka.shared.tiles.domain.model.TilePalette
import com.example.arplitka.shared.tiles.domain.model.TileUnit
import com.example.arplitka.shared.tiles.domain.model.TileVariant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TileDetailsSelectionLogicTest {

    private val tile = Tile(
        id = 1,
        name = "Test",
        description = "Desc",
        manufacturer = "M",
        category = "paving_stones",
        unit = TileUnit.M2,
        material = "Concrete",
        surfaceType = "Rough",
        basePrice = 100.0,
        photos = emptyList(),
        colors = listOf(
            TileColor(1, "Grey", "url1", "#808080", displayOrder = 0),
            TileColor(2, "White", "url2", "#FFFFFF", displayOrder = 1)
        ),
        variants = listOf(
            TileVariant(101, 1, 260, 260, 60, 690.0, 10),
            TileVariant(102, 1, 260, 260, 45, 618.0, 8),
            TileVariant(103, 2, 260, 260, 60, 720.0, 5)
        ),
        layouts = listOf(
            TileLayout(
                id = "default",
                name = "Стандарт",
                defaultTextureUrl = "url1",
                repeatWidthMm = 260,
                repeatHeightMm = 260,
                elements = listOf(
                    TileLayoutElement(
                        elementTypeId = "element_260x260",
                        name = "260x260",
                        widthMm = 260,
                        heightMm = 260,
                        countInRepeat = 1,
                        colorSlotId = "default",
                        colorOptions = listOf(
                            TileElementColorOption(1, "Grey", "#808080"),
                            TileElementColorOption(2, "White", "#FFFFFF")
                        )
                    )
                ),
                palettes = listOf(
                    TilePalette(
                        id = "color_1",
                        name = "Grey",
                        textureUrl = "url1",
                        selectedColorsBySlot = mapOf("default" to 1L)
                    ),
                    TilePalette(
                        id = "color_2",
                        name = "White",
                        textureUrl = "url2",
                        selectedColorsBySlot = mapOf("default" to 2L)
                    )
                )
            )
        )
    )

    @Test
    fun `initial selection picks first palette and first thickness for color`() {
        val layoutId = TileDetailsSelectionLogic.initialLayoutId(tile)
        val paletteId = TileDetailsSelectionLogic.initialPaletteId(tile, layoutId)
        val palette = TileDetailsSelectionLogic.findPalette(
            TileDetailsSelectionLogic.findLayout(tile, layoutId),
            paletteId
        )
        val colorId = TileDetailsSelectionLogic.dominantColorId(palette)
        val thickness = TileDetailsSelectionLogic.initialThicknessMm(tile, colorId)

        assertEquals("default", layoutId)
        assertEquals("color_1", paletteId)
        assertEquals(1L, colorId)
        assertEquals(45, thickness)
    }

    @Test
    fun `selecting thickness changes resolved variant price`() {
        val variant60 = TileDetailsSelectionLogic.resolveVariant(tile, colorId = 1, thicknessMm = 60)
        val variant45 = TileDetailsSelectionLogic.resolveVariant(tile, colorId = 1, thicknessMm = 45)

        assertEquals(690.0, variant60?.price)
        assertEquals(618.0, variant45?.price)
    }

    @Test
    fun `buildPaletteOptions marks selected palette`() {
        val layout = TileDetailsSelectionLogic.findLayout(tile, "default")
        val options = TileDetailsSelectionLogic.buildPaletteOptions(layout, selectedPaletteId = "color_2")

        assertEquals(1, options.count { it.isSelected })
        assertTrue(options.any { it.id == "color_2" && it.isSelected })
    }

    @Test
    fun `availableThicknesses depend on selected color`() {
        val greyThicknesses = TileDetailsSelectionLogic.availableThicknesses(tile, colorId = 1)
        val whiteThicknesses = TileDetailsSelectionLogic.availableThicknesses(tile, colorId = 2)

        assertEquals(listOf(45, 60), greyThicknesses)
        assertEquals(listOf(60), whiteThicknesses)
    }

    @Test
    fun `buildTileSelection contains layout and palette ids`() {
        val selection = TileDetailsSelectionLogic.buildTileSelection(
            tile = tile,
            layoutId = "default",
            paletteId = "color_1",
            selectedColorsBySlot = mapOf("default" to 1L),
            thicknessMm = 60
        )

        assertEquals(1L, selection.tileId)
        assertEquals("default", selection.layoutId)
        assertEquals("color_1", selection.paletteId)
        assertEquals(101L, selection.variantId)
    }
}
