package com.example.arplitka.di

import com.example.arplitka.mock.core.MockProviderImpl
import com.example.arplitka.network.core.HttpClientFactory
import com.example.arplitka.network.core.MockProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMockProvider(): MockProvider = MockProviderImpl

    @Provides
    @Singleton
    fun provideHttpClient(mockProvider: MockProvider): HttpClient {
        return HttpClientFactory.create(mockProvider = mockProvider)
    }
}
