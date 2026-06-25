package com.example.arplitka.shared.tiles.data.remote

import com.example.arplitka.shared.tiles.data.remote.dto.TileDto
import com.example.arplitka.shared.tiles.data.remote.dto.TilesResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class TilesApiImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String = TilesApiConstants.BASE_URL
) : TilesApi {
    override suspend fun getTiles(): TilesResponseDto {
        return httpClient.get("$baseUrl${TilesApiConstants.TILES_PATH}").body()
    }

    override suspend fun getTileById(id: Long): TileDto {
        return httpClient.get("$baseUrl${TilesApiConstants.TILES_PATH}/$id").body()
    }
}
