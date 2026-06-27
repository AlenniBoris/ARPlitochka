package com.example.arplitka.shared.tiles.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TileLayoutDto(
    val id: String,
    val name: String,
    val previewUrl: String? = null,
    val defaultTextureUrl: String,
    val repeatWidthMm: Int,
    val repeatHeightMm: Int,
    val elements: List<TileLayoutElementDto> = emptyList(),
    val palettes: List<TilePaletteDto> = emptyList()
)

@Serializable
data class TileLayoutElementDto(
    val elementTypeId: String,
    val name: String,
    val widthMm: Int,
    val heightMm: Int,
    val countInRepeat: Int,
    val colorSlotId: String,
    val colorOptions: List<TileElementColorOptionDto> = emptyList()
)

@Serializable
data class TileElementColorOptionDto(
    val colorId: Long,
    val name: String,
    val hexCode: String,
    val textureUrl: String? = null,
    val swatchUrl: String? = null,
    val sku: String? = null
)

@Serializable
data class TilePaletteDto(
    val id: String,
    val name: String,
    val textureUrl: String,
    val previewUrl: String? = null,
    val selectedColorsBySlot: Map<String, Long> = emptyMap()
)
