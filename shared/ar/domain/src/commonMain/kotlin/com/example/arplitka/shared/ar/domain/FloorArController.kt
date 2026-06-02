package com.example.arplitka.shared.ar.domain

import com.example.arplitka.shared.ar.contracts.model.ArInstruction
import com.example.arplitka.shared.ar.contracts.model.ArPoint3D
import com.example.arplitka.shared.ar.contracts.model.ArTrackingStatus
import com.example.arplitka.shared.ar.contracts.state.FloorArEvent
import com.example.arplitka.shared.ar.domain.logic.FloorContourReducer
import com.example.arplitka.shared.ar.domain.logic.FloorTrackingReducer
import com.example.arplitka.shared.ar.domain.model.FloorContourUiState
import com.example.arplitka.shared.ar.domain.model.FloorFrameSnapshot
import com.example.arplitka.shared.ar.domain.model.PlacedContourPoint

class FloorArController(
    private val onStateChanged: (FloorContourUiState) -> Unit
) {
    private var state = FloorContourUiState()

    fun currentState(): FloorContourUiState = state

    fun onFrame(
        snapshot: FloorFrameSnapshot,
        updatedPoints: List<PlacedContourPoint>
    ) {
        state = FloorTrackingReducer.applyFrame(
            state = state.copy(placedPoints = updatedPoints),
            snapshot = snapshot
        )
        publish()
    }

    fun onEvent(event: FloorArEvent): List<FloorArEffect> {
        val effects = mutableListOf<FloorArEffect>()
        when (event) {
            FloorArEvent.AddPoint -> effects += handleAddPoint()
            FloorArEvent.UndoPoint -> effects += handleUndoPoint()
            FloorArEvent.Reset -> effects += handleReset()
            FloorArEvent.FinalizeArea -> handleFinalize()
            FloorArEvent.RotateTexture -> Unit
            is FloorArEvent.PlatformPointUpdated -> Unit
        }
        publish()
        return effects
    }

    fun onPointAdded(id: String, position: ArPoint3D) {
        state = state.copy(
            placedPoints = state.placedPoints + PlacedContourPoint(id, position)
        )
        publish()
    }

    private fun handleAddPoint(): List<FloorArEffect> {
        if (state.isFinalized) return emptyList()
        if (state.isPolygonClosed) {
            state = state.copy(
                isFinalized = true,
                trackingStatus = ArTrackingStatus.FINALIZED,
                instruction = ArInstruction.EMPTY
            )
            return emptyList()
        }

        val point = FloorContourReducer.tryAddPoint(state) ?: return emptyList()
        return listOf(FloorArEffect.CreateAnchorAt(point))
    }

    private fun handleUndoPoint(): List<FloorArEffect> {
        if (state.isFinalized || state.placedPoints.isEmpty()) return emptyList()
        val removed = state.placedPoints.last()
        state = state.copy(
            placedPoints = state.placedPoints.dropLast(1),
            isPolygonClosed = false,
            trackingStatus = if (state.isFloorDetected) {
                ArTrackingStatus.FLOOR_DETECTED
            } else {
                state.trackingStatus
            }
        )
        return listOf(FloorArEffect.DetachAnchor(removed.id))
    }

    private fun handleReset(): List<FloorArEffect> {
        val hadPoints = state.placedPoints.isNotEmpty()
        state = FloorContourUiState(
            trackingStatus = state.trackingStatus,
            instruction = state.instruction,
            horizontalPlaneCount = state.horizontalPlaneCount,
            selectedArea = state.selectedArea,
            hasCenterHit = state.hasCenterHit,
            isFloorDetected = state.isFloorDetected,
            focusedLabel = state.focusedLabel,
            currentHitPoint = state.currentHitPoint
        )
        return if (hadPoints) listOf(FloorArEffect.DetachAllAnchors) else emptyList()
    }

    private fun handleFinalize() {
        if (state.isPolygonClosed) {
            state = state.copy(
                isFinalized = true,
                trackingStatus = ArTrackingStatus.FINALIZED,
                instruction = ArInstruction.EMPTY
            )
        }
    }

    private fun publish() {
        onStateChanged(state)
    }
}
