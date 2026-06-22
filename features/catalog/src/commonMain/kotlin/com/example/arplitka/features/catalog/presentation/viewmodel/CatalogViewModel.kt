package com.example.arplitka.features.catalog.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arplitka.shared.core.domain.model.CustomResultModelDomain
import com.example.arplitka.shared.tiles.domain.usecase.GetTilesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

open class CatalogViewModel(
    private val getTilesUseCase: GetTilesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<CatalogUiState>(CatalogUiState.Loading)
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()

    private var _loadingJob: Job? = null

    init {
        loadTiles()
    }

    fun loadTiles() {
        _loadingJob?.cancel()
        _loadingJob = viewModelScope.launch {
            _state.update { CatalogUiState.Loading }
            
            fetchTiles()
        }
    }

    fun refreshTiles() {
        loadTiles()
    }

    private suspend fun fetchTiles() {
        delay(3_000L)
        when (val result = getTilesUseCase()) {
            is CustomResultModelDomain.Success -> {
                _state.update { CatalogUiState.Content(tiles = result.result) }
            }
            is CustomResultModelDomain.Error -> {
                _state.update { CatalogUiState.Error(exception = result.exception) }
            }
        }
    }
}
