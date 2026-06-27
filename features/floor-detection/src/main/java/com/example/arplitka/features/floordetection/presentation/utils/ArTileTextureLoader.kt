package com.example.arplitka.features.floordetection.presentation.utils

import android.content.Context
import android.graphics.Bitmap
import java.io.IOException

internal fun loadTileTextureBitmap(context: Context, textureUrl: String): Bitmap? {
    if (textureUrl.isBlank()) return null

    val assetPath = textureUrl
        .removePrefix("file:///android_asset/")
        .removePrefix("/")

    return try {
        context.assets.open(assetPath).use(::decodePavingBitmap)
    } catch (_: IOException) {
        null
    }
}
