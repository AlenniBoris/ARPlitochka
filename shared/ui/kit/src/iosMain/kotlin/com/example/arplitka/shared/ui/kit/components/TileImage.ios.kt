package com.example.arplitka.shared.ui.kit.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

@Composable
actual fun PlatformTileImage(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier
) {
    AsyncImage(
        model = imageUrl,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}
