package com.example.arplitka.mock.core.dsl

import com.example.arplitka.mock.core.MockResponse
import com.example.arplitka.mock.core.MockResponseRegistry
import com.example.arplitka.mock.core.MockRoute

class MockApplicationDsl(
    private val registry: MockResponseRegistry
) {
    fun api(block: MockApiDsl.() -> Unit) {
        MockApiDsl(registry).block()
    }
}

class MockApiDsl(
    private val registry: MockResponseRegistry
) {
    val get: GetRoutes = GetRoutes

    infix fun MockRoute.reply(assetPath: String) {
        registry.register(MockResponse(route = this, assetPath = assetPath))
    }
}

object GetRoutes

fun mockApplication(
    registry: MockResponseRegistry = MockResponseRegistry(),
    block: MockApplicationDsl.() -> Unit
): MockResponseRegistry {
    MockApplicationDsl(registry).block()
    return registry
}
