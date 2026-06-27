package com.example.arplitka.shared.tiles.domain.usecase

import com.example.arplitka.shared.tiles.domain.model.TileElementColorOption
import com.example.arplitka.shared.tiles.domain.model.TileLayout
import com.example.arplitka.shared.tiles.domain.model.TileLayoutElement
import com.example.arplitka.shared.tiles.domain.model.TileSelection
import kotlin.test.Test
import kotlin.test.assertEquals

class CalculateTileEstimateUseCaseTest {

    private val useCase = CalculateTileEstimateUseCase()

  private val kvadroLayout = TileLayout(
      id = "default_mix",
      name = "Микс",
      defaultTextureUrl = "texture",
      repeatWidthMm = 949,
      repeatHeightMm = 632,
      elements = listOf(
          TileLayoutElement(
              elementTypeId = "kvadro_278x158",
              name = "278x158",
              widthMm = 278,
              heightMm = 158,
              countInRepeat = 4,
              colorSlotId = "large",
              colorOptions = listOf(
                  TileElementColorOption(1, "Gray", "#808080", sku = "kvadro_278x158_gray_brown")
              )
          ),
          TileLayoutElement(
              elementTypeId = "kvadro_265x158",
              name = "265x158",
              widthMm = 265,
              heightMm = 158,
              countInRepeat = 4,
              colorSlotId = "medium",
              colorOptions = listOf(
                  TileElementColorOption(1, "Gray", "#808080", sku = "kvadro_265x158_gray_brown")
              )
          )
      )
  )

    @Test
    fun `estimate scales repeat count by area`() {
        val selection = TileSelection(
            tileId = 1,
            layoutId = "default_mix",
            paletteId = "gray_brown_mix",
            selectedColorsBySlot = mapOf("large" to 1L, "medium" to 1L)
        )

        val lines = useCase.calculate(kvadroLayout, selection, areaM2 = 8.95)

        assertEquals(2, lines.size)
        assertEquals(60, lines[0].estimatedCount)
        assertEquals(60, lines[1].estimatedCount)
        assertEquals("kvadro_278x158_gray_brown", lines[0].sku)
    }
}
