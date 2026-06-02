package com.example.arplitka.shared.ar.domain

sealed interface FloorArEffect {
    data class CreateAnchorAt(val point: com.example.arplitka.shared.ar.contracts.model.ArPoint3D) : FloorArEffect
    data class DetachAnchor(val id: String) : FloorArEffect
    data object DetachAllAnchors : FloorArEffect
}
