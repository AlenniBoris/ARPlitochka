package com.example.arplitka.shared.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun StatusPanel(
    statusText: String,
    instructionText: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.56f), RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = statusText,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = instructionText,
            color = Color.White.copy(alpha = 0.86f),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun BlockingMessage(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodyMedium
            )
            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(18.dp))
                Button(onClick = onAction) {
                    Text(actionText)
                }
            }
        }
    }
}

@Composable
fun DebugPanel(
    debugLines: Map<String, String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.48f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        debugLines.forEach { (label, value) ->
            DebugLine(label, value)
        }
    }
}

@Composable
private fun DebugLine(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "$label:",
            color = Color.White.copy(alpha = 0.72f),
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
