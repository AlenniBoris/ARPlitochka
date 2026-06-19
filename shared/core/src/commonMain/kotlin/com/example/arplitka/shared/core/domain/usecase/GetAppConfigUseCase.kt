package com.example.arplitka.shared.core.domain.usecase

import com.example.arplitka.shared.core.config.AppConfigManager
import kotlinx.coroutines.flow.StateFlow
import com.example.arplitka.shared.core.config.AppConfig

class GetAppConfigUseCase {
    operator fun invoke(): StateFlow<AppConfig> = AppConfigManager.config
}
