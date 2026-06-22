package com.example.arplitka.shared.ui.kit

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import arplitka.shared.ui.kit.generated.resources.Res
import arplitka.shared.ui.kit.generated.resources.ic_loading_tile
import arplitka.shared.ui.kit.generated.resources.progress_bar_description

@Composable
fun AppProgressScreen(
    modifier: Modifier = Modifier.fillMaxSize(),
    iconTint: Color = Color.Black
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer(rotationZ = rotationAngle),
            painter = painterResource(Res.drawable.ic_loading_tile),
            contentDescription = stringResource(Res.string.progress_bar_description),
            tint = iconTint
        )
    }
}
