package com.example.arplitka.shared.tiles.data.remote

import com.example.arplitka.shared.tiles.data.remote.dto.TileDto
import com.example.arplitka.shared.tiles.data.remote.dto.TilesResponseDto

interface TilesApi {
    suspend fun getTiles(): TilesResponseDto
    suspend fun getTileById(id: Long): TileDto
}
