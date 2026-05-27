package com.example.arplitka.features.floordetection.presentation.utils.mappers

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.arplitka.features.floordetection.R
import com.example.arplitka.features.floordetection.domain.model.ArInstruction
import com.example.arplitka.features.floordetection.domain.model.ArStatus

@Composable
fun ArStatus.toText(): String = when (this) {
    ArStatus.INITIALIZATION -> stringResource(R.string.initialization)
    ArStatus.SEARCHING_FLOOR -> stringResource(R.string.status_searching)
    ArStatus.FLOOR_DETECTED -> stringResource(R.string.status_candidate)
    ArStatus.TRACKING_LOST -> stringResource(R.string.status_tracking_lost)
    ArStatus.POLYGON_CLOSED -> stringResource(R.string.polygon_closed)
}

@Composable
fun ArInstruction.toText(): String = when (this) {
    ArInstruction.PLEASE_WAIT -> stringResource(R.string.please_wait)
    ArInstruction.SEARCHING -> stringResource(R.string.instruction_searching)
    ArInstruction.MOVE_PHONE -> stringResource(R.string.instruction_move_phone)
    ArInstruction.DETECTED -> stringResource(R.string.instruction_detected)
    ArInstruction.EMPTY -> ""
}
