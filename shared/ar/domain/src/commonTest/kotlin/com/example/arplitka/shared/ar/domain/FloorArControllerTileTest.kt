package com.example.arplitka.shared.ar.domain

import com.example.arplitka.shared.ar.contracts.model.ArPoint3D
import com.example.arplitka.shared.ar.contracts.state.FloorArEvent
import com.example.arplitka.shared.ar.domain.model.FloorContourUiState
import com.example.arplitka.shared.ar.domain.model.PlacedContourPoint
import com.example.arplitka.shared.ar.domain.model.TextureRotation
import com.example.arplitka.shared.ar.domain.model.TileType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FloorArControllerTileTest {
    @Test
    fun finalizeArea_enablesTileControlsWithoutShowingTile() {
        val controller = finalizedController()

        val state = controller.currentState()
        assertTrue(state.showTileControls)
        assertFalse(state.isTileVisible)
        assertEquals(TextureRotation.DEGREES_0, state.textureRotation)
        assertEquals(TileType.PAVING_STONES_V2, state.selectedTileType)
    }

    @Test
    fun toggleTileVisibility_requiresFinalize() {
        val controller = FloorArController { }
        controller.onPointAdded("p0", ArPoint3D(0f, 0f, 0f))
        controller.onPointAdded("p1", ArPoint3D(1f, 0f, 0f))
        controller.onPointAdded("p2", ArPoint3D(1f, 0f, 1f))

        controller.onEvent(FloorArEvent.ToggleTileVisibility)

        assertFalse(controller.currentState().isTileVisible)
    }

    @Test
    fun toggleTileVisibility_switchesTileOnAndOffAfterFinalize() {
        val controller = finalizedController()

        controller.onEvent(FloorArEvent.ToggleTileVisibility)
        assertTrue(controller.currentState().isTileVisible)

        controller.onEvent(FloorArEvent.ToggleTileVisibility)
        assertFalse(controller.currentState().isTileVisible)
    }

    @Test
    fun rotateTexture_cyclesThroughAnglesOnlyInTileMode() {
        val controller = finalizedController()

        controller.onEvent(FloorArEvent.RotateTexture)
        assertEquals(TextureRotation.DEGREES_0, controller.currentState().textureRotation)

        controller.onEvent(FloorArEvent.ToggleTileVisibility)
        controller.onEvent(FloorArEvent.RotateTexture)
        assertEquals(TextureRotation.DEGREES_45, controller.currentState().textureRotation)

        controller.onEvent(FloorArEvent.RotateTexture)
        assertEquals(TextureRotation.DEGREES_90, controller.currentState().textureRotation)

        controller.onEvent(FloorArEvent.RotateTexture)
        assertEquals(TextureRotation.DEGREES_135, controller.currentState().textureRotation)

        controller.onEvent(FloorArEvent.RotateTexture)
        assertEquals(TextureRotation.DEGREES_0, controller.currentState().textureRotation)
    }

    @Test
    fun changeTileType_cyclesVariantsOnlyInTileMode() {
        val controller = finalizedController()

        controller.onEvent(FloorArEvent.ChangeTileType)
        assertEquals(TileType.PAVING_STONES_V2, controller.currentState().selectedTileType)

        controller.onEvent(FloorArEvent.ToggleTileVisibility)
        controller.onEvent(FloorArEvent.ChangeTileType)
        assertEquals(TileType.PAVING_STONES_V1, controller.currentState().selectedTileType)

        controller.onEvent(FloorArEvent.ChangeTileType)
        assertEquals(TileType.PAVING_STONES_V2, controller.currentState().selectedTileType)
    }

    @Test
    fun tileVisible_hidesContourAndKeepsSectionFill() {
        val controller = finalizedController()
        controller.onEvent(FloorArEvent.ToggleTileVisibility)

        val state = controller.currentState()
        assertTrue(state.isTileVisible)
        assertFalse(state.showContourPoints)
        assertFalse(state.showContourLines)
        assertTrue(state.showSectionFill)
    }

    @Test
    fun reset_clearsTileFields() {
        val controller = finalizedController()
        controller.onEvent(FloorArEvent.ToggleTileVisibility)
        controller.onEvent(FloorArEvent.RotateTexture)
        controller.onEvent(FloorArEvent.ChangeTileType)

        controller.onEvent(FloorArEvent.Reset)

        val state = controller.currentState()
        assertFalse(state.isTileVisible)
        assertEquals(TextureRotation.DEGREES_0, state.textureRotation)
        assertEquals(TileType.PAVING_STONES_V2, state.selectedTileType)
        assertFalse(state.isFinalized)
    }

    private fun finalizedController(): FloorArController {
        val controller = FloorArController { }
        val points = listOf(
            PlacedContourPoint("p0", ArPoint3D(0f, 0f, 0f)),
            PlacedContourPoint("p1", ArPoint3D(1f, 0f, 0f)),
            PlacedContourPoint("p2", ArPoint3D(1f, 0f, 1f))
        )
        controller.setStateForTesting(
            FloorContourUiState(
                placedPoints = points,
                isPolygonClosed = true
            )
        )
        controller.onEvent(FloorArEvent.FinalizeArea)
        return controller
    }
}

class FloorContourUiStateTileVisibilityTest {
    @Test
    fun contourVisibilityFlags_followTileMode() {
        val base = FloorContourUiState(
            placedPoints = listOf(
                PlacedContourPoint("p0", ArPoint3D(0f, 0f, 0f)),
                PlacedContourPoint("p1", ArPoint3D(1f, 0f, 0f)),
                PlacedContourPoint("p2", ArPoint3D(1f, 0f, 1f))
            ),
            isPolygonClosed = true,
            isFinalized = true
        )

        assertTrue(base.showContourPoints)
        assertTrue(base.showContourLines)
        assertTrue(base.showSectionFill)

        val tiled = base.copy(isTileVisible = true)
        assertFalse(tiled.showContourPoints)
        assertFalse(tiled.showContourLines)
        assertTrue(tiled.showSectionFill)
    }
}
