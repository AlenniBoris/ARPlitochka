package com.example.arplitka.shared.core.domain.usecase

import com.example.arplitka.shared.core.config.AppConfigManager

class SetMockEnabledUseCase {
    operator fun invoke(isEnabled: Boolean) {
        AppConfigManager.updateConfig { it.copy(isMockEnabled = isEnabled) }
    }
}
