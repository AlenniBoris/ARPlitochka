package com.example.arplitka.shared.core.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

object AppConfigManager {
    const val DATASTORE_FILE_NAME = "app_config.preferences_pb"

    private var dataStore: DataStore<Preferences>? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val IS_MOCK_ENABLED = booleanPreferencesKey("is_mock_enabled")
    private val API_BASE_URL = stringPreferencesKey("api_base_url")
    private val MOCK_DELAY_MS = longPreferencesKey("mock_delay_ms")

    private val _config = MutableStateFlow(AppConfig())
    val config: StateFlow<AppConfig> = _config

    var isReleaseBuild: Boolean = false

    fun init(dataStore: DataStore<Preferences>, isRelease: Boolean = false) {
        this.dataStore = dataStore
        this.isReleaseBuild = isRelease

        scope.launch {
            dataStore.data.map { prefs ->
                val loadedConfig = AppConfig(
                    isMockEnabled = prefs[IS_MOCK_ENABLED] ?: true,
                    apiBaseUrl = prefs[API_BASE_URL] ?: "https://api.example.com",
                    mockDelayMs = prefs[MOCK_DELAY_MS] ?: 500L
                )
                
                if (isReleaseBuild) {
                    loadedConfig.copy(isMockEnabled = false)
                } else {
                    loadedConfig
                }
            }.collect {
                _config.value = it
            }
        }
    }

    fun updateConfig(update: (AppConfig) -> AppConfig) {
        val current = _config.value
        val next = update(current)
        
        val finalNext = if (isReleaseBuild) next.copy(isMockEnabled = false) else next
        
        _config.value = finalNext
        
        scope.launch {
            dataStore?.edit { prefs ->
                prefs[IS_MOCK_ENABLED] = finalNext.isMockEnabled
                prefs[API_BASE_URL] = finalNext.apiBaseUrl
                prefs[MOCK_DELAY_MS] = finalNext.mockDelayMs
            }
        }
    }
}
