package com.example.arplitka.shared.ar.domain

import com.example.arplitka.shared.ar.contracts.model.ArInstruction
import com.example.arplitka.shared.ar.contracts.model.ArPoint3D
import com.example.arplitka.shared.ar.contracts.model.ArTrackingStatus
import com.example.arplitka.shared.ar.contracts.state.FloorArEvent
import com.example.arplitka.shared.ar.domain.logic.AddPointValidation
import com.example.arplitka.shared.ar.domain.logic.FloorContourReducer
import com.example.arplitka.shared.ar.domain.logic.FloorSnapReducer
import com.example.arplitka.shared.ar.domain.logic.FloorTrackingReducer
import com.example.arplitka.shared.ar.domain.model.FloorContourUiState
import com.example.arplitka.shared.ar.domain.model.FloorFrameSnapshot
import com.example.arplitka.shared.ar.domain.model.PlacedContourPoint
import com.example.arplitka.shared.ar.domain.model.FloorContourUiPublishSnapshot
import com.example.arplitka.shared.ar.domain.model.toUiPublishSnapshot

class FloorArController(
    private val onStateChanged: (FloorContourUiState) -> Unit
) {
    private var state = FloorContourUiState()
    private var lastUiPublishSnapshot: FloorContourUiPublishSnapshot? = null

    fun currentState(): FloorContourUiState = state

    fun onFrame(
        snapshot: FloorFrameSnapshot,
        updatedPoints: List<PlacedContourPoint>
    ) {
        state = FloorTrackingReducer.applyFrame(
            state = state.copy(placedPoints = updatedPoints),
            snapshot = snapshot
        )
        publishIfUiChanged()
    }

    fun onEvent(event: FloorArEvent): List<FloorArEffect> {
        val effects = mutableListOf<FloorArEffect>()
        when (event) {
            FloorArEvent.AddPoint -> effects += handleAddPoint()
            is FloorArEvent.AddPointAt -> effects += handleAddPointAt(event.point)
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

    /** Fast snap/close path for platforms that update the live reticle outside throttled onFrame. */
    fun updateLiveContourPoint(livePoint: ArPoint3D?) {
        if (state.isFinalized || state.placedPoints.isEmpty() || livePoint == null) return
        val base = state.copy(
            currentHitPoint = livePoint,
            hasCenterHit = state.hasCenterHit || state.placedPoints.size >= 2
        )
        val snapped = FloorSnapReducer.applySnap(base).let { result ->
            if (result.isPolygonClosed) {
                result.copy(
                    trackingStatus = ArTrackingStatus.POLYGON_CLOSED,
                    instruction = ArInstruction.CONTOUR_CLOSED
                )
            } else {
                result
            }
        }
        if (snapped.snappedPointIndex == state.snappedPointIndex &&
            snapped.isPolygonClosed == state.isPolygonClosed &&
            snapped.trackingStatus == state.trackingStatus &&
            snapped.instruction == state.instruction
        ) {
            return
        }
        state = snapped
        publishIfUiChanged()
    }

    private fun handleAddPoint(): List<FloorArEffect> =
        state.currentHitPoint?.let { handleAddPointAt(it) } ?: emptyList()

    private fun handleAddPointAt(candidate: ArPoint3D): List<FloorArEffect> {
        if (state.isFinalized) return emptyList()
        if (state.isPolygonClosed) {
            state = state.copy(
                isFinalized = true,
                trackingStatus = ArTrackingStatus.FINALIZED,
                instruction = ArInstruction.EMPTY
            )
            publish()
            return emptyList()
        }

        return when (val validation = FloorContourReducer.validateTapPlacement(state, candidate)) {
            is AddPointValidation.Accepted -> listOf(FloorArEffect.CreateAnchorAt(validation.point))
            is AddPointValidation.Rejected -> emptyList()
        }
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
            largestPlaneAreaM2 = state.largestPlaneAreaM2,
            hasCenterHit = state.hasCenterHit,
            isFloorDetected = state.isFloorDetected,
            focusedLabel = state.focusedLabel,
            currentHitPoint = state.currentHitPoint
        )
        lastUiPublishSnapshot = null
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
        lastUiPublishSnapshot = state.toUiPublishSnapshot()
        onStateChanged(state)
    }

    private fun publishIfUiChanged() {
        val snapshot = state.toUiPublishSnapshot()
        if (snapshot == lastUiPublishSnapshot) return
        lastUiPublishSnapshot = snapshot
        onStateChanged(state)
    }
}
