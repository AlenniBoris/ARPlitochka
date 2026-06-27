package com.example.arplitka.iosapp.presentation.screen

import androidx.compose.runtime.Composable
import com.example.arplitka.shared.ui.navigation.AppNavigator

@Composable
expect fun IosArScreen(
    navigator: AppNavigator,
    initialTileId: Long? = null,
    initialLayoutId: String? = null,
    initialPaletteId: String? = null
)
