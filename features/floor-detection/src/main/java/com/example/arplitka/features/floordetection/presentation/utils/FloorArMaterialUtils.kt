package com.example.arplitka.features.floordetection.presentation.utils

import androidx.compose.ui.graphics.Color
import com.google.android.filament.MaterialInstance
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.node.RenderableNode

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
