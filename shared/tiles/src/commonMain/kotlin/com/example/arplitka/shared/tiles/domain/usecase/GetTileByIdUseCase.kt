package com.example.arplitka.shared.tiles.domain.usecase

import com.example.arplitka.shared.core.domain.model.CustomResultModelDomain
import com.example.arplitka.shared.core.domain.model.CommonException
import com.example.arplitka.shared.tiles.domain.model.Tile
import com.example.arplitka.shared.tiles.domain.repository.TilesRepository

class GetTileByIdUseCase(
    private val repository: TilesRepository
) : suspend (Long) -> CustomResultModelDomain<Tile, CommonException> by repository::getTileById
