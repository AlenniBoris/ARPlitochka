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

@Composable
fun CenterReticle(modifier: Modifier = Modifier, isActive: Boolean) {
    val color = if (isActive) Color.White else Color.White.copy(alpha = 0.55f)
    Canvas(modifier = modifier.size(56.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.18f
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
