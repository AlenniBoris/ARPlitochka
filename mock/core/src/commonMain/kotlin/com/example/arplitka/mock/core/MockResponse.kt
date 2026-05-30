package com.example.arplitka.mock.core

data class MockResponse(
    val route: MockRoute,
    val assetPath: String,
    val statusCode: Int = 200
)
