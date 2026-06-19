package com.example.arplitka.shared.core.config

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlin.test.BeforeTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlin.test.AfterTest

class AppConfigManagerTest {

    private class FakeDataStore : DataStore<Preferences> {
        override val data = kotlinx.coroutines.flow.flowOf(androidx.datastore.preferences.core.emptyPreferences())
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            return androidx.datastore.preferences.core.emptyPreferences()
        }
    }

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        AppConfigManager.isReleaseBuild = false
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads default config`() = runTest(testDispatcher) {
        AppConfigManager.init(FakeDataStore(), isRelease = false)
        
        // Advance until idle to allow the init coroutine to run
        testScheduler.advanceUntilIdle()

        AppConfigManager.config.test {
            val config = awaitItem()
            assertEquals(true, config.isMockEnabled)
            assertEquals(500L, config.mockDelayMs)
        }
    }

    @Test
    fun `isReleaseBuild forces mocks off even if enabled in config`() = runTest(testDispatcher) {
        AppConfigManager.init(FakeDataStore(), isRelease = true)
        
        testScheduler.advanceUntilIdle()

        AppConfigManager.config.test {
            val config = awaitItem()
            assertFalse(config.isMockEnabled, "Mocks must be disabled in release build")
        }
    }

    @Test
    fun `updateConfig updates state flow`() = runTest(testDispatcher) {
        AppConfigManager.init(FakeDataStore(), isRelease = false)
        testScheduler.advanceUntilIdle()

        AppConfigManager.updateConfig { it.copy(mockDelayMs = 1000L) }
        testScheduler.advanceUntilIdle()
        
        AppConfigManager.config.test {
            val config = awaitItem()
            assertEquals(1000L, config.mockDelayMs)
        }
    }
}
