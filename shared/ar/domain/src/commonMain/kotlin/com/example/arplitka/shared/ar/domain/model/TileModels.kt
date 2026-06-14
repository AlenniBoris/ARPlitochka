package com.example.arplitka.shared.ar.domain.model

enum class TextureRotation {
    DEGREES_0,
    DEGREES_45,
    DEGREES_90,
    DEGREES_135;

    val degrees: Int
        get() = ordinal * 45
}

enum class TileType(val resourceName: String) {
    PAVING_STONES_V1("paving_stones_v1"),
    PAVING_STONES_V2("paving_stones_v2")
}
