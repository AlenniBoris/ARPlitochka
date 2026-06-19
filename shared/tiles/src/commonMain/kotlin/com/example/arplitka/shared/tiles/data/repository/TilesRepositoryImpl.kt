package com.example.arplitka.shared.tiles.data.repository

import com.example.arplitka.network.core.ApiResult
import com.example.arplitka.network.core.safeApiCall
import com.example.arplitka.shared.tiles.data.mapper.toDomain
import com.example.arplitka.shared.tiles.data.remote.TilesApi
import com.example.arplitka.shared.tiles.domain.model.Tile
import com.example.arplitka.shared.tiles.domain.repository.TilesRepository

class TilesRepositoryImpl(
    private val api: TilesApi
) : TilesRepository {
    override suspend fun getTiles(): ApiResult<List<Tile>> = safeApiCall {
        api.getTiles().items.map { it.toDomain() }
    }
}
