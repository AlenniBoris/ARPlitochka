package com.example.arplitka.features.floordetection.presentation.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
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
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val tileWidthPx = TILE_TEXTURE_WIDTH_M * outputWidth / widthMeters
    val tileHeightPx = TILE_TEXTURE_HEIGHT_M * outputHeight / heightMeters
    val diagonal = sqrt((outputWidth * outputWidth + outputHeight * outputHeight).toFloat())

    canvas.rotate(rotationDegrees.toFloat(), outputWidth / 2f, outputHeight / 2f)

    var y = -diagonal
    while (y < outputHeight + diagonal) {
        var x = -diagonal
        while (x < outputWidth + diagonal) {
            canvas.drawBitmap(
                this,
                null,
                RectF(x, y, x + tileWidthPx, y + tileHeightPx),
                paint
            )
            x += tileWidthPx
        }
        y += tileHeightPx
    }
    return output
}

internal fun TextureRotation.toDegrees(): Int = when (this) {
    TextureRotation.DEGREES_0 -> 0
    TextureRotation.DEGREES_45 -> 45
    TextureRotation.DEGREES_90 -> 90
    TextureRotation.DEGREES_135 -> 135
}
