package com.example.arplitka.features.catalog.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arplitka.shared.core.domain.model.CustomResultModelDomain
import com.example.arplitka.shared.tiles.domain.usecase.GetTilesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

open class CatalogViewModel(
    private val getTilesUseCase: GetTilesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init {
        loadTiles()
    }

    fun loadTiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            when (val result = getTilesUseCase()) {
                is CustomResultModelDomain.Success -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        tiles = result.result
                    ) }
                }
                is CustomResultModelDomain.Error -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = result.exception
                    ) }
                }
            }
        }
    }
}
