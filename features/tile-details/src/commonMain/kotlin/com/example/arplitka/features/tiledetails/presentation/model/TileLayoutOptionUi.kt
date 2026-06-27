package com.example.arplitka.features.tiledetails.presentation.model

data class TileLayoutOptionUi(
    val id: String,
    val name: String,
    val previewUrl: String,
    val isSelected: Boolean
)

data class TilePaletteOptionUi(
    val id: String,
    val name: String,
    val swatchUrl: String,
    val hexCode: String,
    val isSelected: Boolean
)
