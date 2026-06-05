package com.example.arplitka.features.floordetection.presentation.utils.mappers

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.arplitka.features.floordetection.R
import com.example.arplitka.shared.ar.contracts.model.ArInstruction
import com.example.arplitka.shared.ar.contracts.model.ArTrackingStatus

@Composable
fun ArTrackingStatus.toText(): String = when (this) {
    ArTrackingStatus.INITIALIZING -> stringResource(R.string.initialization)
    ArTrackingStatus.SEARCHING_FLOOR -> stringResource(R.string.status_searching)
    ArTrackingStatus.FLOOR_DETECTED -> stringResource(R.string.status_candidate)
    ArTrackingStatus.TRACKING_LOST -> stringResource(R.string.status_tracking_lost)
    ArTrackingStatus.POLYGON_CLOSED -> stringResource(R.string.polygon_closed)
    ArTrackingStatus.FINALIZED -> "" // Or some other string
}

@Composable
fun ArInstruction.toText(): String = when (this) {
    ArInstruction.PLEASE_WAIT -> stringResource(R.string.please_wait)
    ArInstruction.SEARCHING -> stringResource(R.string.instruction_searching)
    ArInstruction.SURFACE_NEARBY -> stringResource(R.string.instruction_surface_nearby)
    ArInstruction.MOVE_PHONE -> stringResource(R.string.instruction_move_phone)
    ArInstruction.DETECTED -> stringResource(R.string.instruction_detected)
    ArInstruction.CONTOUR_CLOSED -> stringResource(R.string.instruction_contour_closed)
    ArInstruction.CONTOUR_CONFIRMED -> stringResource(R.string.instruction_contour_confirmed)
    ArInstruction.TILE_VISIBLE -> stringResource(R.string.instruction_tile_visible)
    ArInstruction.EMPTY -> ""
}
