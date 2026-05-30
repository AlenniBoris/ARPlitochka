package com.example.arplitka.mock.core

expect object PlatformAssetResolver {
    fun resolveAssetUrl(assetPath: String): String
}
