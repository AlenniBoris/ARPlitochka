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
import androidx.compose.foundation.shape.CircleShape
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
import arplitka.features.tile_details.generated.resources.colors
import com.example.arplitka.features.tiledetails.presentation.model.TileColorOptionUi
import com.example.arplitka.shared.ui.core.format.atomic.parseHexColor
import com.example.arplitka.shared.ui.kit.components.TileImage
import com.example.arplitka.shared.ui.kit.utils.resolveTileImageUrl
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TileDetailsColorSelector(
    colorOptions: List<TileColorOptionUi>,
    onColorSelected: (TileColorOptionUi) -> Unit,
    modifier: Modifier = Modifier
) {
    if (colorOptions.isEmpty()) return

    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.colors),
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF2D3142)
        )

        LazyRow(
            modifier = Modifier.padding(top = 12.dp),
            contentPadding = PaddingValues(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(colorOptions, key = { it.id }) { option ->
                ColorSwatchItem(
                    option = option,
                    onClick = { onColorSelected(option) }
                )
            }
        }
    }
}

@Composable
private fun ColorSwatchItem(
    option: TileColorOptionUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (option.isSelected) Color(0xFFC53030) else Color.Transparent
    val resolvedSwatchUrl = remember(option.id, option.swatchUrl) {
        resolveTileImageUrl(option.swatchUrl)
    }
    val useTextureImage = resolvedSwatchUrl.isNotBlank()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(2.dp, borderColor, CircleShape)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                if (useTextureImage) {
                    TileImage(
                        imageUrl = resolvedSwatchUrl,
                        contentDescription = option.name,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(parseHexColor(option.hexCode))
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClick)
            )
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
