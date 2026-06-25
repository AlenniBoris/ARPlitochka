package com.example.arplitka.features.tiledetails.presentation.screen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import arplitka.features.tile_details.generated.resources.Res
import arplitka.features.tile_details.generated.resources.colors
import com.example.arplitka.shared.ui.core.format.atomic.parseHexColor
import com.example.arplitka.shared.tiles.domain.model.TileColor
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TileColorSwatches(
    colors: List<TileColor>,
    modifier: Modifier = Modifier
) {
    if (colors.isEmpty()) return

    Text(
        text = stringResource(Res.string.colors),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF2D3142),
        modifier = modifier.padding(top = 24.dp)
    )

    Row(
        modifier = Modifier.padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(parseHexColor(color.hexCode))
            )
        }
    }
}
