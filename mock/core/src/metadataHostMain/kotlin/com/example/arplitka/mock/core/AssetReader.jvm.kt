package com.example.arplitka.mock.core

actual object AssetReader {
    actual fun readAsset(path: String): String {
        return "" // Not used in metadataHost
    }

    actual fun resolveAssetPath(path: String): String? {
        return "mock://$path"
    }
}
