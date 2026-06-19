package com.example.arplitka.shared.tiles.domain.usecase

import com.example.arplitka.network.core.ApiResult
import com.example.arplitka.shared.tiles.domain.model.Tile
import com.example.arplitka.shared.tiles.domain.repository.TilesRepository

class GetTilesUseCase(
    repository: TilesRepository
) : suspend () -> ApiResult<List<Tile>> by repository::getTiles
