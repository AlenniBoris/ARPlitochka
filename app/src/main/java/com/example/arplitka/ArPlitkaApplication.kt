package com.example.arplitka

import android.app.Application
import com.example.arplitka.mock.core.AssetReader
import com.example.arplitka.shared.core.config.AppConfigManager
import com.example.arplitka.shared.core.config.createAndroidDataStore
import com.example.arplitka.mock.tiles.initTilesMocks
import com.example.arplitka.BuildConfig
import com.example.arplitka.mock.core.di.mockModule
import com.example.arplitka.features.floordetection.di.floorDetectionModule
import com.example.arplitka.shared.app.di.initKoin
import org.koin.android.ext.koin.androidContext

class ArPlitkaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Инициализация Koin
        initKoin(
            additionalModules = if (BuildConfig.DEBUG) {
                listOf(mockModule, floorDetectionModule)
            } else {
                listOf(floorDetectionModule)
            }
        ) {
            androidContext(this@ArPlitkaApplication)
        }
        
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
