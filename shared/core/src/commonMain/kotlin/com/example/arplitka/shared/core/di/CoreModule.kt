package com.example.arplitka.shared.core.di

import com.example.arplitka.shared.core.domain.usecase.GetAppConfigUseCase
import com.example.arplitka.shared.core.domain.usecase.SetMockDelayUseCase
import com.example.arplitka.shared.core.domain.usecase.SetMockEnabledUseCase
import org.koin.dsl.module

val coreModule = module {
    factory { GetAppConfigUseCase() }
    factory { SetMockDelayUseCase() }
    factory { SetMockEnabledUseCase() }
}
