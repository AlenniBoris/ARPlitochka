package com.example.arplitka.features.floordetection.di

import com.example.arplitka.features.floordetection.data.repository.FloorDetectorRepositoryImpl
import com.example.arplitka.features.floordetection.domain.repository.IFloorDetectorRepository
import com.example.arplitka.features.floordetection.domain.usecase.ProcessArFrameUseCase
import com.example.arplitka.features.floordetection.presentation.viewmodel.FloorArViewModel
import com.example.arplitka.shared.tiles.domain.usecase.BuildArTileTextureUseCase
import com.example.arplitka.shared.tiles.domain.usecase.GetTilesUseCase
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val floorDetectionModule = module {
    single<IFloorDetectorRepository> { FloorDetectorRepositoryImpl() }
    factory { ProcessArFrameUseCase(repository = get()) }
    viewModel { (initialTileId: Long?, initialLayoutId: String?, initialPaletteId: String?) ->
        FloorArViewModel(
            processArFrameUseCase = get(),
            getTilesUseCase = get(),
            buildArTileTextureUseCase = get(),
            initialTileId = initialTileId,
            initialLayoutId = initialLayoutId,
            initialPaletteId = initialPaletteId
        )
    }
}
