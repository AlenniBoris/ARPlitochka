package com.example.arplitka.shared.ar.domain.model

import com.example.arplitka.shared.ar.contracts.model.ArInstruction
import com.example.arplitka.shared.ar.contracts.model.ArTrackingStatus

/**
 * Fields that affect Compose overlays. Excludes per-frame samples such as [FloorContourUiState.currentHitPoint]
 * and anchor-refined point positions (contour renderer reads [FloorArController.currentState] each AR frame).
 */
internal data class FloorContourUiPublishSnapshot(
    val trackingStatus: ArTrackingStatus,
    val instruction: ArInstruction,
    val horizontalPlaneCount: Int,
    val selectedArea: Float,
    val hasCenterHit: Boolean,
    val isFloorDetected: Boolean,
    val focusedLabel: String,
    val placedPointCount: Int,
    val isPolygonClosed: Boolean,
    val isFinalized: Boolean,
    val isTileVisible: Boolean,
    val selectedTileType: TileType,
    val textureRotation: TextureRotation,
    val snappedPointIndex: Int?
)

internal fun FloorContourUiState.toUiPublishSnapshot(): FloorContourUiPublishSnapshot =
    FloorContourUiPublishSnapshot(
        trackingStatus = trackingStatus,
        instruction = instruction,
        horizontalPlaneCount = horizontalPlaneCount,
        selectedArea = selectedArea,
        hasCenterHit = hasCenterHit,
        isFloorDetected = isFloorDetected,
        focusedLabel = focusedLabel,
        placedPointCount = placedPoints.size,
        isPolygonClosed = isPolygonClosed,
        isFinalized = isFinalized,
        isTileVisible = isTileVisible,
        selectedTileType = selectedTileType,
        textureRotation = textureRotation,
        snappedPointIndex = snappedPointIndex
    )
