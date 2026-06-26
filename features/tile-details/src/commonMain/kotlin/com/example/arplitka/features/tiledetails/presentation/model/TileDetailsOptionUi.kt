package com.example.arplitka.features.tiledetails.presentation.model

data class TileColorOptionUi(
    val id: Long,
    val name: String,
    val swatchUrl: String,
    val hexCode: String,
    val isSelected: Boolean
)

data class TileThicknessOptionUi(
    val thicknessMm: Int,
    val label: String,
    val isSelected: Boolean
)
