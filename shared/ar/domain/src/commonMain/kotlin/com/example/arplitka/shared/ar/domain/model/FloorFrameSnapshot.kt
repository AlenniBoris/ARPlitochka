package com.example.arplitka.shared.ar.domain.model

import com.example.arplitka.shared.ar.contracts.model.ArPoint3D

data class FloorFrameSnapshot(
    val isTracking: Boolean,
    val horizontalPlaneCount: Int,
    val selectedArea: Float,
    val hasCenterHit: Boolean,
    val isFloorDetected: Boolean,
    val currentHitPoint: ArPoint3D?,
    val focusedLabel: String = "",
    /** Largest tracked horizontal plane area this frame (scan feedback; may differ from [selectedArea]). */
    val largestPlaneAreaM2: Float = 0f
)
