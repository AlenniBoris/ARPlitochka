package com.example.arplitka.shared.tiles.domain.repository

import com.example.arplitka.shared.core.domain.model.CustomResultModelDomain
import com.example.arplitka.shared.core.domain.model.CommonException
import com.example.arplitka.shared.tiles.domain.model.Tile

interface TilesRepository {
    suspend fun getTiles(): CustomResultModelDomain<List<Tile>, CommonException>
    suspend fun getTileById(id: Long): CustomResultModelDomain<Tile, CommonException>
}
