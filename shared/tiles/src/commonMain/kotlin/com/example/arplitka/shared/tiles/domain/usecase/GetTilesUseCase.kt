package com.example.arplitka.shared.tiles.domain.usecase

import com.example.arplitka.shared.core.domain.model.CustomResultModelDomain
import com.example.arplitka.shared.core.domain.model.CommonException
import com.example.arplitka.shared.tiles.domain.model.Tile
import com.example.arplitka.shared.tiles.domain.repository.TilesRepository

class GetTilesUseCase(
    repository: TilesRepository
) : suspend () -> CustomResultModelDomain<List<Tile>, CommonException> by repository::getTiles
