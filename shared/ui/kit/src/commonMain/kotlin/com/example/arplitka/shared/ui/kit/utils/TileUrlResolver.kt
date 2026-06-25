package com.example.arplitka.shared.ui.kit.utils

import com.example.arplitka.mock.core.AssetReader
import com.example.arplitka.shared.tiles.domain.validation.atomic.url.isRemoteImageUrl

/**
 * Преобразует сырой URL изображения (из API или моков) в путь, понятный загрузчику Coil.
 * Обрабатывает префиксы file:///android_asset/ для корректной работы моков на всех платформах.
 */
fun resolveTileImageUrl(rawUrl: String): String {
    if (rawUrl.isBlank()) return rawUrl
    if (isRemoteImageUrl(rawUrl)) return rawUrl

    val cleanPath = rawUrl.removePrefix("file:///android_asset/").removePrefix("/")
    return AssetReader.resolveAssetPath(cleanPath) ?: rawUrl
}
