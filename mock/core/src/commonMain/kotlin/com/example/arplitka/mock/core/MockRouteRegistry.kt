package com.example.arplitka.mock.core

object MockRouteRegistry {
    private val routes = mutableMapOf<String, JsonAsset>()

    fun register(path: String, asset: JsonAsset) {
        routes[path] = asset
    }

    fun findAsset(path: String): JsonAsset? {
        // Simple exact match for now, can be improved with regex/wildcards
        return routes[path] ?: routes.entries.find { (route, _) -> 
            path.endsWith(route) || route.endsWith(path) 
        }?.value
    }

    fun clear() {
        routes.clear()
    }

    operator fun invoke(block: MockRouteRegistryBuilder.() -> Unit) {
        MockRouteRegistryBuilder(this).apply(block)
    }
}

class MockRouteRegistryBuilder(private val registry: MockRouteRegistry) {
    infix fun String.reply(asset: JsonAsset) {
        registry.register(this, asset)
    }
}
