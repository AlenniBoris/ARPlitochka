package com.example.arplitka.shared.tiles.data.remote

import com.example.arplitka.shared.tiles.data.remote.dto.TileCollectionsResponseDto

interface TilesApi {
    suspend fun getTileCollections(): TileCollectionsResponseDto
}
