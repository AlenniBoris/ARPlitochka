package com.example.arplitka.iosapp

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.example.arplitka.iosapp.presentation.screen.IosArScreen
import com.example.arplitka.shared.app.ArPlitkaSharedApp
import org.koin.compose.KoinContext

@Composable
fun IosApp() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
            }
            .build()
    }
    
    KoinContext {
        ArPlitkaSharedApp(
            arContent = { navigator, arRoute ->
                IosArScreen(
                    navigator = navigator,
                    initialTileId = arRoute.tileId,
                    initialLayoutId = arRoute.layoutId,
                    initialPaletteId = arRoute.paletteId
                )
            }
        )
    }
}
