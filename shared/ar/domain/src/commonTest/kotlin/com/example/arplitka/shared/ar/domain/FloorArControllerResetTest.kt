package com.example.arplitka.shared.ar.domain

import com.example.arplitka.shared.ar.contracts.model.ArPoint3D
import com.example.arplitka.shared.ar.contracts.state.FloorArEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FloorArControllerResetTest {
    @Test
    fun reset_clearsContourAndEmitsDetachAllAnchors() {
        val controller = FloorArController { }
        controller.onPointAdded("p0", ArPoint3D(0f, 0f, 0f))
        controller.onPointAdded("p1", ArPoint3D(1f, 0f, 0f))

        val effects = controller.onEvent(FloorArEvent.Reset)

        assertEquals(1, effects.size)
        assertIs<FloorArEffect.DetachAllAnchors>(effects[0])
        val state = controller.currentState()
        assertTrue(state.placedPoints.isEmpty())
        assertFalse(state.isPolygonClosed)
        assertFalse(state.isFinalized)
        assertTrue(state.showContourActions)
    }

    @Test
    fun reset_preservesTrackingFields() {
        val controller = FloorArController { }
        controller.onPointAdded("p0", ArPoint3D(0f, 0f, 0f))
        val before = controller.currentState()

        controller.onEvent(FloorArEvent.Reset)

        val after = controller.currentState()
        assertEquals(before.trackingStatus, after.trackingStatus)
        assertEquals(before.horizontalPlaneCount, after.horizontalPlaneCount)
        assertEquals(before.hasCenterHit, after.hasCenterHit)
    }

    @Test
    fun reset_withoutPoints_emitsNoEffects() {
        val controller = FloorArController { }
        val effects = controller.onEvent(FloorArEvent.Reset)
        assertTrue(effects.isEmpty())
        assertTrue(controller.currentState().placedPoints.isEmpty())
    }
}
