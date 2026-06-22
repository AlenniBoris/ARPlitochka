package com.example.arplitka.iosapp

import androidx.compose.ui.window.ComposeUIViewController
import com.example.arplitka.mock.core.di.mockModule
import com.example.arplitka.mock.tiles.initTilesMocks
import com.example.arplitka.shared.app.di.initKoin
import com.example.arplitka.shared.core.config.AppConfigManager
import com.example.arplitka.shared.core.config.createIosDataStore
import platform.UIKit.UIViewController

private var isIosAppInitialized = false

fun MainViewController(): UIViewController {
    if (!isIosAppInitialized) {
        initKoin(
            additionalModules = listOf(mockModule)
        )

        val dataStore = createIosDataStore()
        AppConfigManager.init(
            dataStore = dataStore,
            isRelease = false
        )

        initTilesMocks()
        isIosAppInitialized = true
    }

    return ComposeUIViewController {
        IosApp()
    }
}
