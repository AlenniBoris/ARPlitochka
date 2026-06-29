package com.example.arplitka.shared.ui.kit.ar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ArDebugTogglePanel(
    showDebugPanel: Boolean,
    onToggleDebug: () -> Unit,
    debugLines: Map<String, String>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(12.dp)) {
        Button(
            onClick = onToggleDebug,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(text = if (showDebugPanel) "Hide Debug" else "Show Debug")
        }

        if (showDebugPanel) {
            DebugPanel(
                debugLines = debugLines,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
