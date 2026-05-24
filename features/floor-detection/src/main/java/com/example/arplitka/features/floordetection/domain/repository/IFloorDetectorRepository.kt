package com.example.arplitka.features.floordetection.domain.repository

import androidx.compose.ui.unit.IntSize
import com.example.arplitka.features.floordetection.domain.model.ArFrameResult
import com.google.ar.core.Frame
import com.google.ar.core.Session

interface IFloorDetectorRepository {
    /**
     * Processes the current AR frame and returns the detection results.
     */
    fun processFrame(session: Session, frame: Frame, viewportSize: IntSize): ArFrameResult
    
    fun reset()
}
