package com.example.arplitka.features.floordetection.presentation.screen

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import com.example.arplitka.features.floordetection.domain.model.TextureRotation
import kotlin.math.sqrt

internal fun Bitmap.toSectionPatternBitmap(
    widthMeters: Float,
    heightMeters: Float,
    rotationDegrees: Int
): Bitmap {
    val outputWidth = ((width / TILE_TEXTURE_WIDTH_M) * widthMeters)
        .toInt()
        .coerceIn(MIN_SECTION_TEXTURE_SIZE_PX, MAX_SECTION_TEXTURE_SIZE_PX)
    val outputHeight = ((height / TILE_TEXTURE_HEIGHT_M) * heightMeters)
        .toInt()
        .coerceIn(MIN_SECTION_TEXTURE_SIZE_PX, MAX_SECTION_TEXTURE_SIZE_PX)

    val output = Bitmap.createBitmap(outputWidth, outputHeight, config ?: Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val shader = BitmapShader(this, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.shader = shader
    }
    val diagonal = sqrt((outputWidth * outputWidth + outputHeight * outputHeight).toFloat())
    canvas.rotate(rotationDegrees.toFloat(), outputWidth / 2f, outputHeight / 2f)
    canvas.drawRect(-diagonal, -diagonal, outputWidth + diagonal, outputHeight + diagonal, paint)
    return output
}

internal fun TextureRotation.toDegrees(): Int = when (this) {
    TextureRotation.DEGREES_0 -> 0
    TextureRotation.DEGREES_45 -> 45
    TextureRotation.DEGREES_90 -> 90
    TextureRotation.DEGREES_135 -> 135
}
