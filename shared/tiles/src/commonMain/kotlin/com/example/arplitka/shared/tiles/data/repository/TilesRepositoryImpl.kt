package com.example.arplitka.shared.tiles.data.repository

import com.example.arplitka.shared.core.domain.model.CustomResultModelDomain
import com.example.arplitka.shared.core.domain.model.CommonException
import com.example.arplitka.shared.tiles.data.mapper.toDomain
import com.example.arplitka.shared.tiles.data.remote.TilesApi
import com.example.arplitka.shared.tiles.domain.model.Tile
import com.example.arplitka.shared.tiles.domain.repository.TilesRepository
import com.example.arplitka.shared.tiles.domain.mapper.toCommonException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class TilesRepositoryImpl(
    private val api: TilesApi
) : TilesRepository {
    override suspend fun getTiles(): CustomResultModelDomain<List<Tile>, CommonException> = withContext(Dispatchers.IO) {
        try {
            val response = api.getTiles()
            CustomResultModelDomain.Success(response.items.map { it.toDomain() })
        } catch (e: Throwable) {
            CustomResultModelDomain.Error(e.toCommonException())
        }
    }

    override suspend fun getTileById(id: Long): CustomResultModelDomain<Tile, CommonException> = withContext(Dispatchers.IO) {
        try {
            val response = api.getTileById(id)
            CustomResultModelDomain.Success(response.toDomain())
        } catch (e: Throwable) {
            CustomResultModelDomain.Error(e.toCommonException())
        }
    }
}
