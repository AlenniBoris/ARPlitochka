package com.example.arplitka.shared.ui.kit.ar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DebugPanel(
    debugLines: Map<String, String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .widthIn(max = 240.dp)
            .heightIn(max = 200.dp)
            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        debugLines.forEach { (label, value) ->
            DebugLine(label, value)
        }
    }
}

@Composable
private fun DebugLine(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "$label:",
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            maxLines = 1
        )
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            maxLines = 1
        )
    }
}
