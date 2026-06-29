package com.example.arplitka.shared.ui.kit.utils

/**
 * Converts mock/API texture paths to a resource stem understood by native loaders
 * (Android assets path segment or iOS bundle image name).
 */
fun textureUrlToResourceStem(textureUrl: String): String {
    if (textureUrl.isBlank()) return textureUrl

    val cleanPath = textureUrl
        .removePrefix("file:///android_asset/")
        .removePrefix("/")

    val fileName = cleanPath.substringAfterLast('/')
    return fileName.removeSuffix(".png").removeSuffix(".jpg")
}

fun textureRepeatMeters(repeatMm: Int): Float = repeatMm.coerceAtLeast(1).toFloat() / 1000f
