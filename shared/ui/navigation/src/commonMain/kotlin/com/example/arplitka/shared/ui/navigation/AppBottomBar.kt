package com.example.arplitka.shared.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppBottomBar(
    modifier: Modifier = Modifier,
    items: List<BottomBarModelUi>,
    currentRoute: String?
) {
    Row(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) Color(0xFFE8F5E9) else Color.Transparent,
                label = "bottom bar background color",
                animationSpec = tween(500)
            )

            BottomBarItem(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(backgroundColor)
                    .clickable { item.onClick() }
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                item = item,
                isSelected = isSelected
            )
        }
    }
}

@Composable
private fun BottomBarItem(
    modifier: Modifier = Modifier,
    item: BottomBarModelUi,
    isSelected: Boolean
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = item.icon,
            tint = if (isSelected) Color(0xFF2E7D32) else Color.Gray,
            contentDescription = item.text
        )

        Text(
            text = item.text,
            color = if (isSelected) Color(0xFF2E7D32) else Color.Gray,
            fontSize = 12.sp,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
