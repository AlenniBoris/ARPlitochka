package com.example.arplitka.network.core.di

import com.example.arplitka.network.core.HttpClientFactory
import com.example.arplitka.network.core.MockProvider
import org.koin.dsl.module

val networkModule = module {
    single { HttpClientFactory.create(mockProvider = getOrNull<MockProvider>()) }
}
