package com.example.arplitka.shared.ui.kit.ar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun ArActionRail(
    showAddOrChooseTile: Boolean,
    showRemoveTile: Boolean,
    showRotate: Boolean,
    showRescan: Boolean,
    onAddOrChooseTile: () -> Unit,
    onRemoveTile: () -> Unit,
    onRotate: () -> Unit,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (showAddOrChooseTile) {
            ArRailButton(
                icon = if (showRemoveTile) Icons.Default.GridView else Icons.Default.Add,
                contentDescription = if (showRemoveTile) "Сменить плитку" else "Добавить плитку",
                onClick = onAddOrChooseTile
            )
        }
        if (showRemoveTile) {
            ArRailButton(
                icon = Icons.Default.VisibilityOff,
                contentDescription = "Убрать плитку",
                onClick = onRemoveTile
            )
        }
        if (showRotate) {
            ArRailButton(
                icon = Icons.Default.RotateRight,
                contentDescription = "Повернуть",
                onClick = onRotate
            )
        }
        if (showRescan) {
            ArRailButton(
                icon = Icons.Default.Refresh,
                contentDescription = "Удалить зону",
                onClick = onRescan
            )
        }
    }
}

@Composable
private fun ArRailButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.45f),
        modifier = modifier.size(48.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
fun ArZoneResetButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.35f),
        modifier = modifier.size(40.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Удалить зону",
            tint = Color.White,
            modifier = Modifier.padding(10.dp)
        )
    }
}
