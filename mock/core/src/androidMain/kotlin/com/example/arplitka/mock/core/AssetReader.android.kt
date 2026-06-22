package com.example.arplitka.mock.core

import android.content.Context

actual object AssetReader {
    private var applicationContext: Context? = null

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    actual fun readAsset(path: String): String {
        val context = applicationContext ?: throw IllegalStateException("AssetReader not initialized with Context")
        return context.assets.open(path).bufferedReader().use { it.readText() }
    }

    actual fun resolveAssetPath(path: String): String? {
        return "file:///android_asset/$path"
    }
}
