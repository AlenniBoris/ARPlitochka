package com.example.arplitka.shared.tiles.di

import com.example.arplitka.shared.tiles.data.remote.TilesApi
import com.example.arplitka.shared.tiles.data.remote.TilesApiImpl
import com.example.arplitka.shared.tiles.data.repository.TilesRepositoryImpl
import com.example.arplitka.shared.tiles.domain.repository.TilesRepository
import com.example.arplitka.shared.tiles.domain.usecase.GetTilesUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TilesModule {

    @Provides
    @Singleton
    fun provideTilesApi(httpClient: HttpClient): TilesApi = 
        TilesApiImpl(httpClient)

    @Provides
    @Singleton
    fun provideTilesRepository(api: TilesApi): TilesRepository = 
        TilesRepositoryImpl(api)

    @Provides
    @Singleton
    fun provideGetTilesUseCase(repository: TilesRepository): GetTilesUseCase = 
        GetTilesUseCase(repository)
}
