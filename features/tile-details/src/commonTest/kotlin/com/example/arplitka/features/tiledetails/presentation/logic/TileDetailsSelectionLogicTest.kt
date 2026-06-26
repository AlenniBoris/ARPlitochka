package com.example.arplitka.features.tiledetails.presentation.logic

import com.example.arplitka.shared.tiles.domain.model.Tile
import com.example.arplitka.shared.tiles.domain.model.TileColor
import com.example.arplitka.shared.tiles.domain.model.TileElementSize
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
      )
  )

  @Test
  fun `initial selection picks first color and first thickness for color`() {
      val colorId = TileDetailsSelectionLogic.initialColorId(tile)
      val thickness = TileDetailsSelectionLogic.initialThicknessMm(tile, colorId)

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
  fun `buildColorOptions marks selected color`() {
      val options = TileDetailsSelectionLogic.buildColorOptions(tile, selectedColorId = 2)

      assertEquals(1, options.count { it.isSelected })
      assertTrue(options.any { it.id == 2L && it.isSelected })
  }

  @Test
  fun `availableThicknesses depend on selected color`() {
      val greyThicknesses = TileDetailsSelectionLogic.availableThicknesses(tile, colorId = 1)
      val whiteThicknesses = TileDetailsSelectionLogic.availableThicknesses(tile, colorId = 2)

      assertEquals(listOf(45, 60), greyThicknesses)
      assertEquals(listOf(60), whiteThicknesses)
  }
}
