package com.example.arplitka.mock.core

import platform.Foundation.NSBundle

actual object PlatformAssetResolver {
    actual fun resolveAssetUrl(assetPath: String): String {
        val normalizedPath = assetPath.removePrefix("/")
        val fileName = normalizedPath.substringAfterLast("/")
        val directory = normalizedPath.substringBeforeLast("/", missingDelimiterValue = "")
        val name = fileName.substringBeforeLast(".", missingDelimiterValue = fileName)
        val extension = fileName.substringAfterLast(".", missingDelimiterValue = "")
        val resolvedPath = NSBundle.mainBundle.pathForResource(
            name = name,
            ofType = extension,
            inDirectory = directory.ifEmpty { null }
        )
        return resolvedPath?.let { "file://$it" } ?: "mock://$normalizedPath"
    }
}
