package com.example.arplitka.shared.ui.kit.ar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import arplitka.shared.ui.core.generated.resources.Res as SharedRes
import arplitka.shared.ui.core.generated.resources.try_again_string
import com.example.arplitka.shared.ui.core.mapper.formatTilePriceLabel
import com.example.arplitka.shared.ui.kit.components.TileImage
import com.example.arplitka.shared.ui.kit.screens.AppProgressScreen
import com.example.arplitka.shared.ui.kit.utils.resolveTileImageUrl
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArTilePickerBottomSheet(
    state: ArTilePickerState,
    onDismiss: () -> Unit,
    onTileSelected: (Long) -> Unit,
    onLayoutSelected: (String) -> Unit,
    onPaletteSelected: (String) -> Unit,
    onRetryCatalog: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isVisible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Text(
            text = "Выберите плитку",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2D3142),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        when {
            state.isCatalogLoading -> {
                AppProgressScreen(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp)
                )
            }
            state.catalogLoadError != null -> {
                ArPickerCatalogError(
                    error = state.catalogLoadError,
                    onRetry = onRetryCatalog,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp)
                )
            }
            else -> {
                ArTilePickerSheetContent(
                    state = state,
                    onTileSelected = onTileSelected,
                    onLayoutSelected = onLayoutSelected,
                    onPaletteSelected = onPaletteSelected
                )
            }
        }
    }
}

@Composable
private fun ArPickerCatalogError(
    error: com.example.arplitka.shared.ui.core.model.ExceptionModelUi,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(error.exceptionStringResource),
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF2D3142),
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(SharedRes.string.try_again_string),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFC53030),
            modifier = Modifier.clickable(onClick = onRetry)
        )
    }
}

@Composable
internal fun ArTilePickerSheetContent(
    state: ArTilePickerState,
    onTileSelected: (Long) -> Unit,
    onLayoutSelected: (String) -> Unit,
    onPaletteSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.tiles, key = { it.id }) { tile ->
                val isSelected = tile.id == state.selectedTileId
                ArPickerTileCard(
                    tile = tile,
                    isSelected = isSelected,
                    onClick = { onTileSelected(tile.id) },
                    modifier = Modifier.width(148.dp)
                )
            }
        }

        if (state.layouts.size > 1) {
            Text(
                text = "Схема укладки",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF2D3142),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.layouts, key = { it.id }) { layout ->
                    ArPickerChip(
                        label = layout.name,
                        isSelected = layout.id == state.selectedLayoutId,
                        onClick = { onLayoutSelected(layout.id) }
                    )
                }
            }
        }

        if (state.palettes.isNotEmpty()) {
            Text(
                text = "Вариант микса",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF2D3142),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.palettes, key = { it.id }) { palette ->
                    ArPickerPaletteSwatch(
                        palette = palette,
                        onClick = { onPaletteSelected(palette.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ArPickerTileCard(
    tile: ArTileListItemUi,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) Color(0xFFC53030) else Color.Transparent
    val imageUrl = resolveTileImageUrl(tile.imageUrl)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        TileImage(
            imageUrl = imageUrl,
            contentDescription = tile.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
        )
        Text(
            text = tile.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2D3142),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (tile.basePrice != null && tile.unit != null) {
            Text(
                text = formatTilePriceLabel(tile.basePrice, tile.unit),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFC53030),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun ArPickerChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) Color(0xFF2D3142) else Color(0xFFE8E8ED)
    val fg = if (isSelected) Color.White else Color(0xFF2D3142)

    Text(
        text = label,
        color = fg,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
private fun ArPickerPaletteSwatch(
    palette: ArPickerPaletteUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (palette.isSelected) Color(0xFFC53030) else Color.Transparent
    val swatchUrl = resolveTileImageUrl(palette.swatchUrl)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .border(2.dp, borderColor, CircleShape)
                .clickable(onClick = onClick)
                .padding(3.dp),
            contentAlignment = Alignment.Center
        ) {
            if (swatchUrl.isNotBlank()) {
                TileImage(
                    imageUrl = swatchUrl,
                    contentDescription = palette.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.Gray)
                )
            }
        }
        Text(
            text = palette.name,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF4A4E69),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
