package com.example.arplitka.features.catalog.di

import com.example.arplitka.features.catalog.presentation.viewmodel.CatalogViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val catalogModule = module {
    viewModel { CatalogViewModel(getTilesUseCase = get()) }
}
