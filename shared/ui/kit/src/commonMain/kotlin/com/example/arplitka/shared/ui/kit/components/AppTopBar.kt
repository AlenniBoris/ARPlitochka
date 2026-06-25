package com.example.arplitka.shared.ui.kit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AppTopBar(
    title: String? = null,
    leftIcon: ImageVector? = null,
    onLeftClick: (() -> Unit)? = null,
    rightIcon: ImageVector? = null,
    onRightClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    contentColor: Color = Color(0xFF2D3142),
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leftIcon != null && onLeftClick != null) {
            IconButton(onClick = onLeftClick) {
                Icon(
                    imageVector = leftIcon,
                    contentDescription = null,
                    tint = contentColor
                )
            }
        }

        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            )
        } else {
            Box(modifier = Modifier.weight(1f))
        }

        actions()

        if (rightIcon != null && onRightClick != null) {
            IconButton(onClick = onRightClick) {
                Icon(
                    imageVector = rightIcon,
                    contentDescription = null,
                    tint = contentColor
                )
            }
        }
    }
}
