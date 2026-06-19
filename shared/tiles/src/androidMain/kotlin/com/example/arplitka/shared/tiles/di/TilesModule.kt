package com.example.arplitka.shared.tiles.di

import com.example.arplitka.shared.tiles.data.repository.TilesRepositoryImpl
import com.example.arplitka.shared.tiles.domain.repository.TilesRepository
import com.example.arplitka.shared.tiles.domain.usecase.GetTilesUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TilesModule {

    @Provides
    @Singleton
    fun provideTilesRepository(impl: TilesRepositoryImpl): TilesRepository = impl

    @Provides
    @Singleton
    fun provideGetTilesUseCase(repository: TilesRepository): GetTilesUseCase = 
        GetTilesUseCase(repository)
}
