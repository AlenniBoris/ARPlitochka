package com.example.arplitka.shared.core.domain.usecase

import com.example.arplitka.shared.core.config.AppConfigManager

class SetMockDelayUseCase {
    operator fun invoke(delayMs: Long) {
        AppConfigManager.updateConfig { it.copy(mockDelayMs = delayMs) }
    }
}
