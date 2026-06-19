package com.example.arplitka.iosapp

import androidx.compose.ui.window.ComposeUIViewController
import com.example.arplitka.shared.core.config.AppConfigManager
import com.example.arplitka.shared.core.config.createIosDataStore
import com.example.arplitka.mock.tiles.initTilesMocks
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    // Инициализация конфигурации через DataStore для iOS
    val dataStore = createIosDataStore()
    AppConfigManager.init(
        dataStore = dataStore,
        isRelease = false // Можно настроить передачу флага из Swift при необходимости
    )
    
    initTilesMocks()
    
    return ComposeUIViewController {
        IosApp()
    }
}
