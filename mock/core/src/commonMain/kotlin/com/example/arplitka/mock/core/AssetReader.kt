package com.example.arplitka.mock.core

expect object AssetReader {
    fun readAsset(path: String): String
}
