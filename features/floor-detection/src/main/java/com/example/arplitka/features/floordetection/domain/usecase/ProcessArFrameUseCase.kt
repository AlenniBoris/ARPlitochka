package com.example.arplitka.features.floordetection.domain.usecase

import androidx.compose.ui.unit.IntSize
import com.example.arplitka.features.floordetection.domain.model.ArFrameResult
import com.example.arplitka.features.floordetection.domain.repository.IFloorDetectorRepository
import com.google.ar.core.Frame
import com.google.ar.core.Session
import javax.inject.Inject

/**
 * Use case for processing an AR frame to detect the floor.
 */
class ProcessArFrameUseCase @Inject constructor(
    private val repository: IFloorDetectorRepository
) {
    operator fun invoke(session: Session, frame: Frame, viewportSize: IntSize): ArFrameResult {
        return repository.processFrame(session, frame, viewportSize)
    }
}
