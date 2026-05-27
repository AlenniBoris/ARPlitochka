package com.example.arplitka.features.floordetection.data.repository

import androidx.compose.ui.unit.IntSize
import com.example.arplitka.features.floordetection.domain.model.ArFrameResult
import com.example.arplitka.features.floordetection.domain.repository.IFloorDetectorRepository
import com.example.arplitka.shared.ar.core.area
import com.example.arplitka.shared.ar.core.centerPlaneHit
import com.example.arplitka.shared.ar.core.isUsableHorizontalPlane
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import javax.inject.Inject

class FloorDetectorRepositoryImpl @Inject constructor() : IFloorDetectorRepository {

    override fun processFrame(session: Session, frame: Frame, viewportSize: IntSize): ArFrameResult {
        val trackingState = frame.camera.trackingState
        val allHorizontalPlanes = session.getAllTrackables(Plane::class.java)
            .filter { it.isUsableHorizontalPlane() }
        
        val centerHit = if (trackingState == TrackingState.TRACKING) {
            frame.centerPlaneHit(viewportSize)
        } else {
            null
        }

        if (trackingState != TrackingState.TRACKING) {
            return ArFrameResult(
                trackingState = trackingState,
                horizontalPlaneCount = allHorizontalPlanes.size,
                selectedArea = 0f,
                hasCenterHit = false,
                isFloorDetected = false,
                isDepthEnabled = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
            )
        }

        val selectedPlane = (centerHit?.trackable as? Plane)?.takeIf { plane ->
            plane.isUsableHorizontalPlane() && plane.area() >= MIN_FLOOR_AREA_M2
        }

        return ArFrameResult(
            trackingState = trackingState,
            horizontalPlaneCount = allHorizontalPlanes.size,
            selectedArea = selectedPlane?.area() ?: (allHorizontalPlanes.maxOfOrNull { it.area() } ?: 0f),
            hasCenterHit = centerHit != null,
            isFloorDetected = selectedPlane != null,
            isDepthEnabled = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC),
            hitPose = centerHit?.hitPose,
            hitResult = centerHit
        )
    }

    override fun reset() {
        // No state to reset in the repository anymore
    }

    companion object {
        private const val MIN_FLOOR_AREA_M2 = 0.15f
    }
}
