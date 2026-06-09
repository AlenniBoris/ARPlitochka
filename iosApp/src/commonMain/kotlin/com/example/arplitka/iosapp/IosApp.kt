package com.example.arplitka.iosapp

import androidx.compose.runtime.Composable
import com.example.arplitka.shared.app.ArPlitkaSharedApp
import com.example.arplitka.iosapp.presentation.screen.IosArScreen

@Composable
fun IosApp() {
    ArPlitkaSharedApp(
        arContent = { onBack ->
            IosArScreen(onBack = onBack)
        }
    )
}
