package com.example.arplitka.shared.ui.kit.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRefreshIndicator(
    modifier: Modifier = Modifier,
    state: PullToRefreshState,
    isRefreshing: Boolean,
    durationMillis: Int = 1200
) {
    val indicatorColor = Color.Black
    val containerColor = Color.White

    val bgColor by animateColorAsState(
        targetValue = if (isRefreshing) indicatorColor else containerColor,
        animationSpec = tween(durationMillis = durationMillis)
    )

    val contentColor by animateColorAsState(
        targetValue = if (isRefreshing) Color.White else indicatorColor,
        animationSpec = tween(durationMillis = durationMillis)
    )

    val distanceFraction = { state.distanceFraction.coerceIn(0f, 1f) }

    Box(
        modifier = modifier
            .graphicsLayer {
                val progress = distanceFraction()
                this.alpha = if (isRefreshing) 1f else progress
                this.scaleX = if (isRefreshing) 1f else progress
                this.scaleY = if (isRefreshing) 1f else progress
                this.shadowElevation = if (isRefreshing) 6.dp.toPx() else (6.dp.toPx() * progress)
                this.shape = CircleShape
                this.clip = true
            }
            .background(bgColor)
            .padding(10.dp)
            .pullToRefresh(
                state = state,
                isRefreshing = isRefreshing,
                onRefresh = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            modifier = Modifier.align(Alignment.Center),
            targetState = isRefreshing,
            animationSpec = tween(durationMillis = durationMillis)
        ) { refreshing ->
            if (refreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = contentColor
                )
            }
        }
    }
}
