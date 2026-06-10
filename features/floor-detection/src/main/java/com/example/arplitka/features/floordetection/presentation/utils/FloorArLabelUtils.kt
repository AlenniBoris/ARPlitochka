package com.example.arplitka.features.floordetection.presentation.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import java.util.Locale

internal fun createLineStripBitmap(): Bitmap = createSolidStripBitmap(color = "#FF2196F3")

internal fun createPreviewLineStripBitmap(): Bitmap = createSolidStripBitmap(color = "#FF00A2FF")

private fun createSolidStripBitmap(color: String): Bitmap {
    val width = 64
    val height = 16
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
        Canvas(bitmap).drawColor(android.graphics.Color.parseColor(color))
    }
}

internal fun createDistanceLabelBitmap(text: String): Bitmap {
    val width = 320
    val height = 112
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        textSize = 38f
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    canvas.drawRoundRect(
        RectF(0f, 0f, width.toFloat(), height.toFloat()),
        28f,
        28f,
        backgroundPaint
    )
    val textBaseline = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(text, width / 2f, textBaseline, textPaint)
    return bitmap
}

internal fun Float.formatMeters(): String {
    return String.format(Locale.getDefault(), "%.2f м", this)
}
