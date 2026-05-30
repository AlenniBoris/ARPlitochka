package com.example.arplitka.iosapp

import androidx.compose.runtime.Composable
import com.example.arplitka.shared.app.ArPlitkaSharedApp

@Composable
fun IosApp() {
    ArPlitkaSharedApp(
        arContent = { onBack ->
            IosArScreen(onBack = onBack)
        }
    )
}

@Composable
expect fun IosArScreen(onBack: () -> Unit)
