package com.example.arplitka.shared.ar.domain.model

import com.example.arplitka.shared.ar.contracts.model.ArInstruction
import com.example.arplitka.shared.ar.contracts.model.ArPoint3D
import com.example.arplitka.shared.ar.contracts.model.ArTrackingStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class FloorContourUiPublishTest {
    @Test
    fun publishSnapshot_ignoresPerFrameHitPointChanges() {
        val base = FloorContourUiState(
            trackingStatus = ArTrackingStatus.FLOOR_DETECTED,
            instruction = ArInstruction.DETECTED,
            hasCenterHit = true,
            isFloorDetected = true
        )
        val movedHit = base.copy(currentHitPoint = ArPoint3D(1f, 0f, 0f))
        val otherHit = base.copy(currentHitPoint = ArPoint3D(2f, 0f, 2f))
        assertEquals(movedHit.toUiPublishSnapshot(), otherHit.toUiPublishSnapshot())
    }

    @Test
    fun publishSnapshot_changesWhenTrackingStatusChanges() {
        val searching = FloorContourUiState(trackingStatus = ArTrackingStatus.SEARCHING_FLOOR)
        val detected = searching.copy(trackingStatus = ArTrackingStatus.FLOOR_DETECTED)
        assertNotEquals(
            searching.toUiPublishSnapshot(),
            detected.toUiPublishSnapshot()
        )
    }

    @Test
    fun publishSnapshot_changesWhenTileFieldsChange() {
        val base = FloorContourUiState(
            isFinalized = true,
            isPolygonClosed = true,
            isTileVisible = false
        )
        val tiled = base.copy(isTileVisible = true, textureRotation = TextureRotation.DEGREES_90)
        assertNotEquals(base.toUiPublishSnapshot(), tiled.toUiPublishSnapshot())
    }
}
