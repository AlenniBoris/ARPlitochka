package com.example.arplitka.shared.ui.kit.ar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.arplitka.shared.ui.kit.components.TileImage
import com.example.arplitka.shared.ui.kit.utils.resolveTileImageUrl

@Composable
fun ArColorRail(
    palettes: List<ArPickerPaletteUi>,
    onPaletteSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (palettes.isEmpty()) return

    LazyColumn(
        modifier = modifier
            .padding(8.dp)
            .heightIn(max = 220.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(palettes, key = { it.id }) { palette ->
            ArColorRailSwatch(
                palette = palette,
                onClick = { onPaletteSelected(palette.id) }
            )
        }
    }
}

@Composable
private fun ArColorRailSwatch(
    palette: ArPickerPaletteUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (palette.isSelected) Color(0xFFC53030) else Color.Transparent
    val swatchUrl = resolveTileImageUrl(palette.swatchUrl)

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .border(2.dp, borderColor, CircleShape)
            .background(Color.Black.copy(alpha = 0.35f), CircleShape)
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
}
