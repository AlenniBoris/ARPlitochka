package com.example.arplitka.features.floordetection.domain.model

import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState

enum class FloorDetectionState {
    SearchingFloor,
    CandidateFound
}

enum class TextureRotation {
    DEGREES_0,
    DEGREES_45,
    DEGREES_90,
    DEGREES_135
}

enum class TileType(val assetPath: String) {
    CLASSIC("textures/paving_stones.png"),
    MODERN("textures/paving_stones_v2.png")
}

enum class ArStatus {
    INITIALIZATION,
    SEARCHING_FLOOR,
    FLOOR_DETECTED,
    TRACKING_LOST,
    POLYGON_CLOSED
}

enum class ArInstruction {
    PLEASE_WAIT,
    SEARCHING,
    MOVE_PHONE,
    DETECTED,
    EMPTY
}

data class ArPoint(
    val anchor: Anchor,
    val pose: Pose,
    val id: Long = System.nanoTime()
)

data class FloorUiState(
    val detectionState: FloorDetectionState = FloorDetectionState.SearchingFloor,
    val trackingState: TrackingState = TrackingState.STOPPED,
    val horizontalPlaneCount: Int = 0,
    val selectedArea: Float = 0f,
    val hasCenterHit: Boolean = false,
    val isDepthEnabled: Boolean = false,
    val status: ArStatus = ArStatus.INITIALIZATION,
    val instruction: ArInstruction = ArInstruction.PLEASE_WAIT,
    val points: List<ArPoint> = emptyList(),
    val isPolygonClosed: Boolean = false,
    val isFinalized: Boolean = false,
    val textureRotation: TextureRotation = TextureRotation.DEGREES_0,
    val selectedTileType: TileType = TileType.MODERN,
    val currentHitPose: Pose? = null,
    val currentHitResult: HitResult? = null,
    val snappedPointIndex: Int? = null
)

data class ArFrameResult(
    val trackingState: TrackingState,
    val horizontalPlaneCount: Int,
    val selectedArea: Float,
    val hasCenterHit: Boolean,
    val isFloorDetected: Boolean,
    val isDepthEnabled: Boolean,
    val hitPose: Pose? = null,
    val hitResult: HitResult? = null
)
