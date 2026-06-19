package com.example.arplitka.shared.tiles.data.mapper

import com.example.arplitka.shared.tiles.data.remote.dto.TileColorDto
import com.example.arplitka.shared.tiles.data.remote.dto.TileDto
import com.example.arplitka.shared.tiles.data.remote.dto.TileVariantDto
import com.example.arplitka.shared.tiles.domain.model.TileUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class TileDtoMapperTest {

    @Test
    fun `toDomain maps TileDto correctly`() {
        val dto = TileDto(
            id = 1L,
            name = "Test Tile",
            description = "Test Description",
            manufacturer = "Test Manufacturer",
            category = "test_cat",
            unit = "m2",
            material = "Ceramic",
            surfaceType = "Glossy",
            basePrice = 100.0,
            photos = listOf("url1", "url2"),
            colors = listOf(
                TileColorDto(1, "Red", "url_red", "#FF0000")
            ),
            variants = listOf(
                TileVariantDto(101, 1, 300, 300, 10, 100.0, 50, 10)
            )
        )

        val domain = dto.toDomain()

        assertEquals(dto.id, domain.id)
        assertEquals(dto.name, domain.name)
        assertEquals(TileUnit.M2, domain.unit)
        assertEquals(1, domain.colors.size)
        assertEquals("Red", domain.colors[0].name)
        assertEquals(1, domain.variants.size)
        assertEquals(300, domain.variants[0].widthMm)
    }

    @Test
    fun `toDomain maps TileUnit correctly for different strings`() {
        assertEquals(TileUnit.M2, TileDto(1, "", "", "", "", "m2", "", "", 0.0, emptyList(), emptyList(), emptyList()).toDomain().unit)
        assertEquals(TileUnit.PIECE, TileDto(1, "", "", "", "", "piece", "", "", 0.0, emptyList(), emptyList(), emptyList()).toDomain().unit)
        assertEquals(TileUnit.BOX, TileDto(1, "", "", "", "", "box", "", "", 0.0, emptyList(), emptyList(), emptyList()).toDomain().unit)
        assertEquals(TileUnit.M2, TileDto(1, "", "", "", "", "unknown", "", "", 0.0, emptyList(), emptyList(), emptyList()).toDomain().unit)
    }
}
