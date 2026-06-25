package com.example.arplitka.shared.ui.kit.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.example.arplitka.shared.tiles.domain.validation.atomic.url.isRemoteImageUrl

@Composable
fun TileImage(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    if (isRemoteImageUrl(imageUrl)) {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        PlatformTileImage(
            imageUrl = imageUrl,
            contentDescription = contentDescription,
            modifier = modifier
        )
    }
}

@Composable
expect fun PlatformTileImage(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier
)
