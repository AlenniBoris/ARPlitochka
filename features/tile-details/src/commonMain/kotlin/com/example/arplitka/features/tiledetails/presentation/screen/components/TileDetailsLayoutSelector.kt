package com.example.arplitka.features.tiledetails.presentation.screen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import arplitka.features.tile_details.generated.resources.Res
import arplitka.features.tile_details.generated.resources.layout
import com.example.arplitka.features.tiledetails.presentation.model.TileLayoutOptionUi
import com.example.arplitka.shared.ui.kit.components.TileImage
import com.example.arplitka.shared.ui.kit.utils.resolveTileImageUrl
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TileDetailsLayoutSelector(
    layoutOptions: List<TileLayoutOptionUi>,
    onLayoutSelected: (TileLayoutOptionUi) -> Unit,
    modifier: Modifier = Modifier
) {
    if (layoutOptions.size <= 1) return

    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.layout),
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF2D3142)
        )

        LazyRow(
            modifier = Modifier.padding(top = 12.dp),
            contentPadding = PaddingValues(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(layoutOptions, key = { it.id }) { option ->
                LayoutPreviewItem(
                    option = option,
                    onClick = { onLayoutSelected(option) }
                )
            }
        }
    }
}

@Composable
private fun LayoutPreviewItem(
    option: TileLayoutOptionUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (option.isSelected) Color(0xFFC53030) else Color(0xFFE0E0E0)
    val resolvedPreviewUrl = remember(option.id, option.previewUrl) {
        resolveTileImageUrl(option.previewUrl)
    }

    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(2.dp, borderColor, RoundedCornerShape(8.dp))
                .background(Color(0xFFF5F5F5))
        ) {
            if (resolvedPreviewUrl.isNotBlank()) {
                TileImage(
                    imageUrl = resolvedPreviewUrl,
                    contentDescription = option.name,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            }
        }

        Text(
            text = option.name,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF4A4E69),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
