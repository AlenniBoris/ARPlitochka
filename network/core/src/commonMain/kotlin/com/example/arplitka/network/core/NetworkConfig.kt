package com.example.arplitka.network.core

data class NetworkConfig(
    val baseUrl: String,
    val isMockModeEnabledByDefault: Boolean = true
)
