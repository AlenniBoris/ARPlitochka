package com.example.arplitka.shared.ui.core.format.atomic

import androidx.compose.ui.graphics.Color

fun parseHexColor(hex: String, fallback: Color = Color.Gray): Color {
    return try {
        Color(hex.removePrefix("#").toLong(16) or 0x00000000FF000000L)
    } catch (_: Exception) {
        fallback
    }
}
