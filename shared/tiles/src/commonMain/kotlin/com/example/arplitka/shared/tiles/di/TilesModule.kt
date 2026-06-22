package com.example.arplitka.shared.tiles.di

import com.example.arplitka.shared.tiles.data.remote.TilesApi
import com.example.arplitka.shared.tiles.data.remote.TilesApiImpl
import com.example.arplitka.shared.tiles.data.repository.TilesRepositoryImpl
import com.example.arplitka.shared.tiles.domain.repository.TilesRepository
import com.example.arplitka.shared.tiles.domain.usecase.GetTilesUseCase
import org.koin.dsl.module

val tilesModule = module {
    single<TilesApi> { TilesApiImpl(httpClient = get()) }
    single<TilesRepository> { TilesRepositoryImpl(api = get()) }
    factory { GetTilesUseCase(repository = get()) }
}
