package com.example.arplitka.shared.tiles.domain.repository

import com.example.arplitka.network.core.ApiResult
import com.example.arplitka.shared.tiles.domain.model.TileCollection

interface TilesRepository {
    suspend fun getTileCollections(): ApiResult<List<TileCollection>>
}
