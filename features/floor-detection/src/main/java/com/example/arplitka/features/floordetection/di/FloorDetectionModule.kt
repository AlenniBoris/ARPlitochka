package com.example.arplitka.features.floordetection.di

import com.example.arplitka.features.floordetection.data.repository.FloorDetectorRepositoryImpl
import com.example.arplitka.features.floordetection.domain.repository.IFloorDetectorRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
abstract class FloorDetectionModule {
    
    @Binds
    @ViewModelScoped
    abstract fun bindFloorDetectorRepository(
        impl: FloorDetectorRepositoryImpl
    ): IFloorDetectorRepository
}
