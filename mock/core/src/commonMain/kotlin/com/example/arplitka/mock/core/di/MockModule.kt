package com.example.arplitka.mock.core.di

import com.example.arplitka.mock.core.MockProviderImpl
import com.example.arplitka.network.core.MockProvider
import org.koin.dsl.module

val mockModule = module {
    single<MockProvider> { MockProviderImpl }
}
