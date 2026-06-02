package com.example.arplitka.features.floordetection.presentation.utils.mappers

import androidx.compose.ui.test.junit4.createComposeRule
import com.example.arplitka.features.floordetection.domain.model.ArInstruction
import com.example.arplitka.features.floordetection.domain.model.ArStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FloorArStateMapperTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun arStatus_toText_returnsCorrectString() {
        composeTestRule.setContent {
            assertEquals("Пожалуйста, подождите", ArStatus.INITIALIZATION.toText())
            assertEquals("Наведите камеру на пол", ArStatus.SEARCHING_FLOOR.toText())
            assertEquals("Пол обнаружен", ArStatus.FLOOR_DETECTED.toText())
            assertEquals("Трекинг потерян", ArStatus.TRACKING_LOST.toText())
            assertEquals("Контур замкнут", ArStatus.POLYGON_CLOSED.toText())
        }
    }

    @Test
    fun arInstruction_toText_returnsCorrectString() {
        composeTestRule.setContent {
            assertEquals("Пожалуйста, подождите", ArInstruction.PLEASE_WAIT.toText())
            assertEquals("Ищем подходящую поверхность", ArInstruction.SEARCHING.toText())
            assertEquals("Медленно наведите камеру на пол", ArInstruction.MOVE_PHONE.toText())
            assertEquals("Поверхность отображается точками", ArInstruction.DETECTED.toText())
            assertEquals("Нажмите OK, чтобы завершить разметку", ArInstruction.CONTOUR_CLOSED.toText())
            assertEquals("Нажмите «Добавить плитку» для предпросмотра", ArInstruction.CONTOUR_CONFIRMED.toText())
            assertEquals("Плитка наложена. Можно повернуть или сменить", ArInstruction.TILE_VISIBLE.toText())
            assertEquals("", ArInstruction.EMPTY.toText())
        }
    }
}
