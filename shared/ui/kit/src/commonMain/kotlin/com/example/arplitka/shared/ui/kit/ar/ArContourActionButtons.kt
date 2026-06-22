package com.example.arplitka.shared.ui.kit.ar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ArContourActionButtons(
    hasCenterHit: Boolean,
    isPolygonClosed: Boolean,
    hasPoints: Boolean,
    onAddPoint: () -> Unit,
    onUndoPoint: () -> Unit,
    addContentDescription: String = "Add point",
    undoContentDescription: String = "Undo",
    okContentDescription: String = "OK",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasPoints) {
            Button(
                onClick = onUndoPoint,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White
                ),
                modifier = Modifier.size(56.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = undoContentDescription
                )
            }
        }

        Button(
            onClick = onAddPoint,
            enabled = hasCenterHit,
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPolygonClosed) Color(0xFF4CAF50) else Color.White,
                contentColor = if (isPolygonClosed) Color.White else Color.Black,
                disabledContainerColor = Color.White.copy(alpha = 0.3f)
            ),
            modifier = Modifier.size(if (isPolygonClosed) 80.dp else 72.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = if (isPolygonClosed) Icons.Default.Check else Icons.Default.Add,
                contentDescription = if (isPolygonClosed) okContentDescription else addContentDescription,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
