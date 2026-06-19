package com.example.arplitka.mock.core

import com.example.arplitka.network.core.MockProvider
import com.example.arplitka.shared.core.config.AppConfigManager

object MockProviderImpl : MockProvider {
    override fun isMockEnabled(): Boolean = AppConfigManager.config.value.isMockEnabled
    
    override fun getMockResponse(path: String): String? {
        return MockRouteRegistry.findAsset(path)?.read()
    }

    override fun getDelayMs(): Long = AppConfigManager.config.value.mockDelayMs
}
