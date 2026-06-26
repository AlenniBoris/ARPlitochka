package com.example.arplitka.shared.ui.kit.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import platform.UIKit.UIImage
import platform.UIKit.UIImageView
import platform.UIKit.UIViewContentMode

@Composable
actual fun PlatformTileImage(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier
) {
    val filePath = imageUrl.removePrefix("file://")
    UIKitView(
        factory = {
            UIImageView().apply {
                contentMode = UIViewContentMode.UIViewContentModeScaleAspectFill
                clipsToBounds = true
                userInteractionEnabled = false
                image = UIImage.imageWithContentsOfFile(filePath)
            }
        },
        update = { imageView ->
            imageView.userInteractionEnabled = false
            imageView.image = UIImage.imageWithContentsOfFile(filePath)
        },
        modifier = modifier,
        properties = UIKitInteropProperties(
            interactionMode = null,
        ),
    )
}
