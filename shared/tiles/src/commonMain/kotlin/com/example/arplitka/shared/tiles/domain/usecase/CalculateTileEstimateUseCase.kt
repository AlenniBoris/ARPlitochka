package com.example.arplitka.shared.tiles.domain.usecase

import com.example.arplitka.shared.tiles.domain.model.TileEstimateLine
import com.example.arplitka.shared.tiles.domain.model.TileLayout
import com.example.arplitka.shared.tiles.domain.model.TileSelection
import kotlin.math.ceil

class CalculateTileEstimateUseCase {

    fun calculate(
        layout: TileLayout,
        selection: TileSelection,
        areaM2: Double
    ): List<TileEstimateLine> {
        if (areaM2 <= 0 || layout.elements.isEmpty()) {
            return emptyList()
        }

        val repeatAreaM2 = layout.repeatWidthMm * layout.repeatHeightMm / 1_000_000.0
        if (repeatAreaM2 <= 0) {
            return emptyList()
        }

        val repeatCount = areaM2 / repeatAreaM2

        return layout.elements.map { element ->
            val colorId = selection.selectedColorsBySlot[element.colorSlotId]
                ?: element.colorOptions.firstOrNull()?.colorId
                ?: 0L

            val colorOption = element.colorOptions.find { it.colorId == colorId }
                ?: element.colorOptions.firstOrNull()

            val estimatedCount = ceil(repeatCount * element.countInRepeat).toInt()

            TileEstimateLine(
                sku = colorOption?.sku ?: element.elementTypeId,
                name = colorOption?.name ?: element.name,
                elementTypeId = element.elementTypeId,
                colorId = colorId,
                estimatedCount = estimatedCount,
                widthMm = element.widthMm,
                heightMm = element.heightMm
            )
        }
    }
}
