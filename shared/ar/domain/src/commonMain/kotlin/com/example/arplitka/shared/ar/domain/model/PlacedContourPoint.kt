package com.example.arplitka.shared.ar.domain.model

import com.example.arplitka.shared.ar.contracts.model.ArPoint3D

data class PlacedContourPoint(
    val id: String,
    val position: ArPoint3D
)
