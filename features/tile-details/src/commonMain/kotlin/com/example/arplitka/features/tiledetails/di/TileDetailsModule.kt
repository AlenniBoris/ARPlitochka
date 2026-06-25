package com.example.arplitka.features.tiledetails.di

import com.example.arplitka.features.tiledetails.presentation.viewmodel.TileDetailsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val tileDetailsModule = module {
    viewModel { (tileId: Long) ->
        TileDetailsViewModel(
            tileId = tileId,
            getTileByIdUseCase = get()
        )
    }
}
