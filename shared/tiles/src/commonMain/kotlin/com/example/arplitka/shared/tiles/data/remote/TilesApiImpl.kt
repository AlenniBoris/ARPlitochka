package com.example.arplitka.shared.tiles.data.remote

import com.example.arplitka.shared.tiles.data.remote.dto.TilesResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class TilesApiImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String = "https://api.example.com" // Placeholder
) : TilesApi {
    override suspend fun getTiles(): TilesResponseDto {
        return httpClient.get("$baseUrl/tiles").body()
    }
}
