package com.example.arplitka.shared.tiles.domain.usecase

import com.example.arplitka.shared.tiles.domain.repository.TilesRepository

class GetTilesUseCase(
    private val repository: TilesRepository
) {
    suspend operator fun invoke() = repository.getTileCollections()
}
