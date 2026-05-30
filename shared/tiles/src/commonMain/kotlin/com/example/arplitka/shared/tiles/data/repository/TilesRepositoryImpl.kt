package com.example.arplitka.shared.tiles.data.repository

import com.example.arplitka.network.core.ApiResult
import com.example.arplitka.network.core.NetworkError
import com.example.arplitka.shared.tiles.data.mapper.toDomain
import com.example.arplitka.shared.tiles.data.remote.TilesApi
import com.example.arplitka.shared.tiles.domain.model.TileCollection
import com.example.arplitka.shared.tiles.domain.repository.TilesRepository

class TilesRepositoryImpl(
    private val api: TilesApi
) : TilesRepository {
    override suspend fun getTileCollections(): ApiResult<List<TileCollection>> {
        return try {
            ApiResult.Success(api.getTileCollections().items.map { it.toDomain() })
        } catch (throwable: Throwable) {
            ApiResult.Error(
                NetworkError(
                    message = throwable.message ?: "Failed to load tile collections",
                    cause = throwable
                )
            )
        }
    }
}
