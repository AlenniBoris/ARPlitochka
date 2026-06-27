package com.example.arplitka.features.floordetection.domain.model

import com.example.arplitka.shared.ar.contracts.model.ArInstruction
import com.example.arplitka.shared.ar.contracts.model.ArTrackingStatus
import com.example.arplitka.shared.ar.contracts.model.ArTileTexture
import com.example.arplitka.shared.ar.domain.model.FloorWorkflowStage
import com.example.arplitka.shared.tiles.domain.model.TileSelection
import kotlin.math.sqrt
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

data class ArPoint(
    val anchor: Anchor,
    val pose: Pose,
    val id: Long = System.nanoTime()
)

data class FloorUiState(
    val stage: FloorWorkflowStage = FloorWorkflowStage.INITIALIZING,
    val detectionState: FloorDetectionState = FloorDetectionState.SearchingFloor,
    val trackingState: TrackingState = TrackingState.STOPPED,
    val horizontalPlaneCount: Int = 0,
    val selectedArea: Float = 0f,
    val hasCenterHit: Boolean = false,
    val isDepthEnabled: Boolean = false,
    val status: ArTrackingStatus = ArTrackingStatus.INITIALIZING,
    val instruction: ArInstruction = ArInstruction.PLEASE_WAIT,
    val points: List<ArPoint> = emptyList(),
    val isPolygonClosed: Boolean = false,
    val isContourConfirmed: Boolean = false,
    val isTileVisible: Boolean = false,
    val textureRotation: TextureRotation = TextureRotation.DEGREES_0,
    val selectedTileType: TileType = TileType.MODERN,
    val tileSelection: TileSelection? = null,
    val arTileTexture: ArTileTexture? = null,
    val pendingAutoApplyTile: Boolean = false,
    val selectedTileName: String? = null,
    val currentHitPose: Pose? = null,
    val currentHitResult: HitResult? = null,
    val snappedPointIndex: Int? = null
) {
    /** Android parity with shared `FloorContourUiState`: points stay until tile mode. */
    val showContourPoints: Boolean
        get() = points.isNotEmpty() && stage != FloorWorkflowStage.TILE_LAYOUT

    val showContourLines: Boolean
        get() = points.size >= 2 && stage != FloorWorkflowStage.TILE_LAYOUT

    val showPlaneRenderer: Boolean
        get() = stage.ordinal < FloorWorkflowStage.CONTOUR_CONFIRMED.ordinal

    /** Preview fill on close (`Closed: Yes`), same as shared/iOS contract. */
    val showSectionFill: Boolean
        get() = (stage == FloorWorkflowStage.CONTOUR_CLOSED || stage.ordinal >= FloorWorkflowStage.CONTOUR_CONFIRMED.ordinal) && points.size >= 3
}

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
