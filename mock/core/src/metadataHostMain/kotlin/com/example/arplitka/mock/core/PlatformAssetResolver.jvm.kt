package com.example.arplitka.mock.core

actual object PlatformAssetResolver {
    actual fun resolveAssetUrl(assetPath: String): String {
        return "mock://$assetPath"
    }
}
