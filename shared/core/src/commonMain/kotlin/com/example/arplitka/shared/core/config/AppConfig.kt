package com.example.arplitka.shared.core.config

data class AppConfig(
    val isMockEnabled: Boolean = true,
    val apiBaseUrl: String = "https://api.example.com",
    val mockDelayMs: Long = 500
)
