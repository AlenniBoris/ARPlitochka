package com.example.arplitka.features.tiledetails.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arplitka.features.tiledetails.presentation.TileDetailsEvent
import com.example.arplitka.features.tiledetails.presentation.model.TileColorOptionUi
import com.example.arplitka.features.tiledetails.presentation.model.TileThicknessOptionUi
import com.example.arplitka.shared.core.domain.model.CustomResultModelDomain
import com.example.arplitka.shared.core.domain.presentation.SingleFlowEvent
import com.example.arplitka.shared.tiles.domain.model.Tile
import com.example.arplitka.shared.tiles.domain.usecase.GetTileByIdUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TileDetailsViewModel(
    private val tileId: Long,
    private val getTileByIdUseCase: GetTileByIdUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<TileDetailsUiState>(TileDetailsUiState.Loading)
    val state: StateFlow<TileDetailsUiState> = _state.asStateFlow()

    private val _event = SingleFlowEvent<TileDetailsEvent>(viewModelScope)
    val event = _event.flow

    init {
        loadTile()
    }

    fun loadTile() {
        viewModelScope.launch {
            _state.update { TileDetailsUiState.Loading }
            delay(100L)
            when (val result = getTileByIdUseCase(tileId)) {
                is CustomResultModelDomain.Success -> {
                    _state.update { TileDetailsUiState.Content.initial(result.result) }
                }
                is CustomResultModelDomain.Error -> {
                    _state.update { TileDetailsUiState.Error(exception = result.exception) }
                }
            }
        }
    }

    fun selectColor(colorOption: TileColorOptionUi) {
        updateContent { content ->
            val newThicknessMm = content.tile.variants
                .filter { it.colorId == colorOption.id }
                .map { it.thicknessMm }
                .distinct()
                .sorted()
                .firstOrNull()
                ?: content.tile.variants.firstOrNull()?.thicknessMm
                ?: 0

            content.copy(
                selectedColorId = colorOption.id,
                selectedThicknessMm = newThicknessMm
            )
        }
    }

    fun selectThickness(thicknessOption: TileThicknessOptionUi) {
        updateContent { content ->
            content.copy(selectedThicknessMm = thicknessOption.thicknessMm)
        }
    }

    fun toggleDescriptionExpanded() {
        updateContent { content ->
            content.copy(isDescriptionExpanded = !content.isDescriptionExpanded)
        }
    }

    fun openWebsite() {
        // Первый инкремент: кнопка без действия.
    }

    fun onTryInAr() {
        _event.emit(TileDetailsEvent.OpenAr)
    }

    fun onBack() {
        _event.emit(TileDetailsEvent.NavigateBack)
    }

    private fun updateContent(transform: (TileDetailsUiState.Content) -> TileDetailsUiState.Content) {
        val current = _state.value
        if (current is TileDetailsUiState.Content) {
            _state.update { transform(current) }
        }
    }
}
