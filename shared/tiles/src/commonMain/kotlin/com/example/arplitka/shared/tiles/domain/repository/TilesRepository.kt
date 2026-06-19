package com.example.arplitka.shared.tiles.domain.repository

import com.example.arplitka.network.core.ApiResult
import com.example.arplitka.shared.tiles.domain.model.Tile

interface TilesRepository {
    suspend fun getTiles(): ApiResult<List<Tile>>
}
