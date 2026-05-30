package com.example.arplitka.features.floordetection.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.arplitka.shared.ar.contracts.model.ArInstruction
import com.example.arplitka.shared.ar.contracts.model.ArTrackingStatus
import com.example.arplitka.features.floordetection.presentation.utils.mappers.toText
import com.example.arplitka.shared.ui.kit.StatusPanel

@Composable
fun ArStatusOverlay(
    status: ArTrackingStatus,
    instruction: ArInstruction,
    modifier: Modifier = Modifier
) {
    StatusPanel(
        statusText = status.toText(),
        instructionText = instruction.toText(),
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 36.dp)
    )
}
