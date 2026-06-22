package com.example.arplitka.features.catalog.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.UIKitView
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.example.arplitka.shared.tiles.domain.validation.atomic.url.isRemoteImageUrl
import platform.UIKit.UIImage
import platform.UIKit.UIImageView
import platform.UIKit.UIViewContentMode

@Composable
internal actual fun TilePreviewImage(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier
) {
    if (isRemoteImageUrl(imageUrl)) {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            onState = { state ->
                when (state) {
                    is AsyncImagePainter.State.Success -> {
                        val size = state.painter.intrinsicSize
                        println("IMAGE STATE: Success path=$imageUrl, size=${size.width}x${size.height}")
                    }
                    is AsyncImagePainter.State.Error -> {
                        println("IMAGE STATE: Error path=$imageUrl, error=${state.result.throwable.message}")
                    }
                    is AsyncImagePainter.State.Loading -> {
                        println("IMAGE STATE: Loading path=$imageUrl")
                    }
                    else -> Unit
                }
            }
        )
        return
    }

    val filePath = imageUrl.removePrefix("file://")
    if (filePath.contains("android_asset")) {
        println("IMAGE ERROR: Path still contains android_asset: $filePath. Asset resolution failed.")
    }
    UIKitView(
        factory = {
            UIImageView().apply {
                contentMode = UIViewContentMode.UIViewContentModeScaleAspectFill
                clipsToBounds = true
                image = UIImage.imageWithContentsOfFile(filePath)
                println("IMAGE DEBUG: UIKitView loaded path=$filePath, success=${image != null}")
            }
        },
        update = { imageView ->
            imageView.image = UIImage.imageWithContentsOfFile(filePath)
        },
        modifier = modifier
    )
}
