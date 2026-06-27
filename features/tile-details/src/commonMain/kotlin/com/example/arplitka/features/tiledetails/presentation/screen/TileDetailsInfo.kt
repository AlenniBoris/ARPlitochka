package com.example.arplitka.features.tiledetails.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.arplitka.features.tiledetails.presentation.screen.components.TileDetailsLayoutSelector
import com.example.arplitka.features.tiledetails.presentation.screen.components.TileDetailsPaletteSelector
import com.example.arplitka.features.tiledetails.presentation.screen.components.TileDetailsExpandableDescription
import com.example.arplitka.features.tiledetails.presentation.screen.components.TileDetailsGallery
import com.example.arplitka.features.tiledetails.presentation.screen.components.TileDetailsPriceRow
import com.example.arplitka.features.tiledetails.presentation.screen.components.TileDetailsSpecsTable
import com.example.arplitka.features.tiledetails.presentation.screen.components.TileDetailsThicknessSelector
import com.example.arplitka.features.tiledetails.presentation.model.TileLayoutOptionUi
import com.example.arplitka.features.tiledetails.presentation.model.TilePaletteOptionUi
import com.example.arplitka.features.tiledetails.presentation.model.TileThicknessOptionUi
import com.example.arplitka.features.tiledetails.presentation.viewmodel.TileDetailsUiState

@Composable
internal fun TileDetailsInfo(
    state: TileDetailsUiState.Content,
    onLayoutSelected: (TileLayoutOptionUi) -> Unit,
    onPaletteSelected: (TilePaletteOptionUi) -> Unit,
    onThicknessSelected: (TileThicknessOptionUi) -> Unit,
    onToggleDescriptionExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tile = state.tile

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp)
    ) {
        TileDetailsGallery(
            photos = tile.photos,
            contentDescription = tile.name
        )

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = tile.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D3142),
                modifier = Modifier.padding(top = 16.dp)
            )

            TileDetailsExpandableDescription(
                description = tile.description,
                isExpanded = state.isDescriptionExpanded,
                onToggleExpanded = onToggleDescriptionExpanded,
                modifier = Modifier.padding(top = 12.dp)
            )

            if (state.showLayoutSelector) {
                TileDetailsLayoutSelector(
                    layoutOptions = state.layoutOptions,
                    onLayoutSelected = onLayoutSelected,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }

            TileDetailsPaletteSelector(
                paletteOptions = state.paletteOptions,
                onPaletteSelected = onPaletteSelected,
                modifier = Modifier.padding(top = 24.dp)
            )

            TileDetailsThicknessSelector(
                thicknessOptions = state.thicknessOptions,
                onThicknessSelected = onThicknessSelected,
                modifier = Modifier.padding(top = 24.dp)
            )

            val price = state.selectedVariant?.price ?: tile.basePrice
            TileDetailsPriceRow(
                price = price,
                unit = tile.unit,
                modifier = Modifier.padding(top = 24.dp)
            )

            TileDetailsSpecsTable(
                tile = tile,
                selectedColor = state.selectedColor,
                selectedThickness = state.selectedThickness,
                selectedVariant = state.selectedVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
