package com.example.arplitka.features.floordetection.di

import com.example.arplitka.features.floordetection.data.repository.FloorDetectorRepositoryImpl
import com.example.arplitka.features.floordetection.domain.repository.IFloorDetectorRepository
import com.example.arplitka.features.floordetection.domain.usecase.ProcessArFrameUseCase
import com.example.arplitka.features.floordetection.presentation.viewmodel.FloorArViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val floorDetectionModule = module {
    single<IFloorDetectorRepository> { FloorDetectorRepositoryImpl() }
    factory { ProcessArFrameUseCase(repository = get()) }
    viewModel { FloorArViewModel(processArFrameUseCase = get()) }
}
