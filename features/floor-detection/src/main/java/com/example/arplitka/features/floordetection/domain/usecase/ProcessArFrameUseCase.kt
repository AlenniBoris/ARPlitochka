package com.example.arplitka.features.floordetection.domain.usecase

import androidx.compose.ui.unit.IntSize
import com.example.arplitka.features.floordetection.domain.model.ArFrameResult
import com.example.arplitka.features.floordetection.domain.repository.IFloorDetectorRepository
import com.google.ar.core.Frame
import com.google.ar.core.Session
import javax.inject.Inject

/**
 * Use case for processing an AR frame to detect the floor.
 * Uses the proxy pattern (delegation) as it has no own logic.
 */
class ProcessArFrameUseCase @Inject constructor(
    repository: IFloorDetectorRepository
) : (Session, Frame, IntSize) -> ArFrameResult by repository::processFrame
