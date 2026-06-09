package com.example.arplitka.shared.ui.kit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

enum class CenterReticleState {
    INACTIVE,
    VALID,
    /** Reticle near an existing point — move away before placing another. */
    SNAP,
    INVALID,
    CLOSED
}

@Composable
fun CenterReticle(
    modifier: Modifier = Modifier,
    isActive: Boolean,
    state: CenterReticleState = if (isActive) CenterReticleState.VALID else CenterReticleState.INACTIVE
) {
    val color = when (state) {
        CenterReticleState.VALID -> Color(0xFF69F0AE)
        CenterReticleState.SNAP -> Color(0xFFFFD54F)
        CenterReticleState.INVALID -> Color(0xFFFF6E6E)
        CenterReticleState.CLOSED -> Color(0xFF00E676)
        CenterReticleState.INACTIVE -> Color.White.copy(alpha = 0.55f)
    }
    val zoneAlpha = when (state) {
        CenterReticleState.VALID -> 0.16f
        CenterReticleState.SNAP -> 0.14f
        CenterReticleState.INVALID -> 0.13f
        CenterReticleState.CLOSED -> 0.18f
        CenterReticleState.INACTIVE -> 0.07f
    }
    Canvas(modifier = modifier.size(88.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.12f
        val placementRadius = size.minDimension * 0.30f
        drawCircle(
            color = color.copy(alpha = zoneAlpha),
            radius = placementRadius,
            center = center
        )
        drawCircle(
            color = color.copy(alpha = if (state == CenterReticleState.INACTIVE) 0.20f else 0.55f),
            radius = placementRadius,
            center = center,
            style = Stroke(width = 2.5f)
        )
        drawCircle(
            color = color.copy(alpha = 0.35f),
            radius = radius,
            center = center,
            style = Stroke(width = 2.5f)
        )
        drawLine(
            color = color,
            start = Offset(center.x - radius - 10f, center.y),
            end = Offset(center.x - radius + 2f, center.y),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(center.x + radius - 2f, center.y),
            end = Offset(center.x + radius + 10f, center.y),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(center.x, center.y - radius - 10f),
            end = Offset(center.x, center.y - radius + 2f),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(center.x, center.y + radius - 2f),
            end = Offset(center.x, center.y + radius + 10f),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    }
}
