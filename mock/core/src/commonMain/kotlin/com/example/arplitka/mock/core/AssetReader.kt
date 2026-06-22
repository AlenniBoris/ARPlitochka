package com.example.arplitka.mock.core

expect object AssetReader {
    fun readAsset(path: String): String
    fun resolveAssetPath(path: String): String?
}
