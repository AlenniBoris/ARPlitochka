package com.example.arplitka.network.core

interface MockProvider {
    fun isMockEnabled(): Boolean
    fun getMockResponse(path: String): String?
    fun getDelayMs(): Long
}
