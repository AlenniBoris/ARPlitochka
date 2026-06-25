package com.example.arplitka.shared.app.di

import com.example.arplitka.features.catalog.di.catalogModule
import com.example.arplitka.features.tiledetails.di.tileDetailsModule
import com.example.arplitka.network.core.di.networkModule
import com.example.arplitka.shared.core.di.coreModule
import com.example.arplitka.shared.tiles.di.tilesModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

fun initKoin(
    additionalModules: List<Module> = emptyList(),
    appDeclaration: KoinAppDeclaration = {}
) = startKoin {
    appDeclaration()
    modules(
        coreModule,
        networkModule,
        tilesModule,
        catalogModule,
        tileDetailsModule
    )
    modules(additionalModules)
}
