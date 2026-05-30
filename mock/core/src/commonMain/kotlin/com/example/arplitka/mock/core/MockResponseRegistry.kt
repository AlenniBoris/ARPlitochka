package com.example.arplitka.mock.core

class MockResponseRegistry {
    private val responses = mutableMapOf<MockRoute, MockResponse>()

    fun register(response: MockResponse) {
        responses[response.route] = response
    }

    fun find(route: MockRoute): MockResponse? = responses[route]

    fun clear() {
        responses.clear()
    }
}
