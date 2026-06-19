package com.example.arplitka

import android.app.Application
import com.example.arplitka.mock.core.AssetReader
import com.example.arplitka.shared.core.config.AppConfigManager
import com.example.arplitka.shared.core.config.createAndroidDataStore
import com.example.arplitka.mock.tiles.initTilesMocks
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ArPlitkaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Инициализация конфигурации через DataStore
        val dataStore = createAndroidDataStore(this)
        AppConfigManager.init(
            dataStore = dataStore,
            isRelease = !BuildConfig.DEBUG
        )
        
        // Инициализация системы чтения ассетов
        AssetReader.init(this)
        initTilesMocks()
    }
}
