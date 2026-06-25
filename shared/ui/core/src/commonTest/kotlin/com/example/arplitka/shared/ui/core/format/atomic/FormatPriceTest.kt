package com.example.arplitka.shared.ui.core.format.atomic

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatPriceTest {

    @Test
    fun formatsWholeNumberWithoutDecimalPart() {
        assertEquals("1500", formatPrice(1500.0))
    }

    @Test
    fun formatsFractionalPrice() {
        assertEquals("99.5", formatPrice(99.5))
    }
}
