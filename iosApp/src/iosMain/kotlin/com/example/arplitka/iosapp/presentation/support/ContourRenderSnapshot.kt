package com.example.arplitka.iosapp.presentation.support

import com.example.arplitka.shared.ar.domain.model.FloorContourUiState
import com.example.arplitka.shared.ar.domain.model.PlacedContourPoint

internal enum class ContourRenderSyncSource {
    DELEGATE,
    RENDER_LOOP,
    MANUAL_REALIGN,
    TAP
}

internal data class ContourRenderSnapshot(
    val version: Long,
    val syncSource: ContourRenderSyncSource,
    val uiState: FloorContourUiState,
    val placedPoints: List<PlacedContourPoint>,
    val sectionFloorY: Float?
) {
    val syncSourceLabel: String
        get() = when (syncSource) {
            ContourRenderSyncSource.DELEGATE -> "delegate"
            ContourRenderSyncSource.RENDER_LOOP -> "renderLoop"
            ContourRenderSyncSource.MANUAL_REALIGN -> "manualRealign"
            ContourRenderSyncSource.TAP -> "tap"
        }
}
