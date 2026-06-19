package com.example.arplitka.mock.core

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MockRouteRegistryTest {

    @BeforeTest
    fun setup() {
        MockRouteRegistry.clear()
    }

    @Test
    fun `findAsset returns correct asset for registered path`() {
        val asset = JsonAsset("test.json")
        MockRouteRegistry {
            "/api/test" reply asset
        }

        val found = MockRouteRegistry.findAsset("/api/test")
        assertNotNull(found)
        assertEquals(asset.path, found.path)
    }

    @Test
    fun `findAsset returns null for unregistered path`() {
        val found = MockRouteRegistry.findAsset("/unknown")
        assertNull(found)
    }

    @Test
    fun `findAsset handles partial matches if configured`() {
        val asset = JsonAsset("tiles.json")
        MockRouteRegistry {
            "/tiles" reply asset
        }

        // Should match because of endsWith logic in registry
        assertNotNull(MockRouteRegistry.findAsset("https://api.com/tiles"))
        assertNotNull(MockRouteRegistry.findAsset("/api/v1/tiles"))
    }
}
