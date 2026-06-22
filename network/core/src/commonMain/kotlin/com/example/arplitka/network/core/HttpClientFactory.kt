package com.example.arplitka.network.core

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

object HttpClientFactory {
    fun create(
        json: Json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            prettyPrint = true
        },
        mockProvider: MockProvider? = null
    ): HttpClient {
        // Используем MockEngine для всех запросов в дебаг режиме, 
        // так как это самый надежный способ мокирования в Ktor KMP.
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    val path = request.url.segments.joinToString("/", prefix = "/")
                    
                    if (mockProvider?.isMockEnabled() == true) {
                        val mockContent = mockProvider.getMockResponse(path)
                        if (mockContent != null) {
                            if ((mockProvider?.getDelayMs() ?: 0) > 0) {
                                delay(mockProvider!!.getDelayMs())
                            }
                            respond(
                                content = ByteReadChannel(mockContent),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            )
                        } else {
                            respond(
                                content = ByteReadChannel("{\"error\": \"Mock not found for $path\"}"),
                                status = HttpStatusCode.NotFound,
                                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            )
                        }
                    } else {
                        respond(
                            content = ByteReadChannel("{\"error\": \"Real network not implemented in mock mode\"}"),
                            status = HttpStatusCode.NotImplemented,
                            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        )
                    }
                }
            }

            install(ContentNegotiation) {
                json(json)
            }

            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println("HTTP Client: $message")
                    }
                }
                level = LogLevel.ALL
            }
        }
    }
}
