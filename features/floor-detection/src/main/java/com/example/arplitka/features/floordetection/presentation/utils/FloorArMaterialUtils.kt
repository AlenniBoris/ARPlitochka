package com.example.arplitka.features.floordetection.presentation.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.node.RenderableNode
import java.io.InputStream

internal const val GUIDE_RENDER_PRIORITY = 7

internal fun MaterialLoader.createGuideOverlayInstance(color: Color): MaterialInstance =
    createUnlitColorInstance(color).apply {
        configureGuideOverlayMaterial()
    }

internal fun MaterialInstance.configureGuideOverlayMaterial() {
    setDepthWrite(false)
    setDepthCulling(false)
}

internal fun RenderableNode.configureGuideOverlayNode() {
    setPriority(GUIDE_RENDER_PRIORITY)
    setCulling(false)
    materialInstance?.configureGuideOverlayMaterial()
}

internal fun decodePavingBitmap(inputStream: InputStream): Bitmap =
    requireNotNull(
        BitmapFactory.decodeStream(
            inputStream,
            null,
            BitmapFactory.Options().apply { inScaled = false }
        )
    ) { "Failed to decode paving bitmap" }

internal fun MaterialLoader.createSectionFillTextureInstance(texture: Texture): MaterialInstance =
    createImageInstance(
        imageTexture = texture,
        sampler = TextureSampler(
            TextureSampler.MinFilter.LINEAR,
            TextureSampler.MagFilter.LINEAR,
            TextureSampler.WrapMode.CLAMP_TO_EDGE
        )
    )
