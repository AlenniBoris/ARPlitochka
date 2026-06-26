package com.example.arplitka.features.tiledetails.presentation.screen.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import arplitka.features.tile_details.generated.resources.Res
import arplitka.features.tile_details.generated.resources.thickness
import com.example.arplitka.features.tiledetails.presentation.model.TileThicknessOptionUi
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TileDetailsThicknessSelector(
    thicknessOptions: List<TileThicknessOptionUi>,
    onThicknessSelected: (TileThicknessOptionUi) -> Unit,
    modifier: Modifier = Modifier
) {
    if (thicknessOptions.isEmpty()) return

    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.thickness),
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF2D3142)
        )

        LazyRow(
            modifier = Modifier.padding(top = 12.dp),
            contentPadding = PaddingValues(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(thicknessOptions, key = { it.thicknessMm }) { option ->
                ThicknessChip(
                    option = option,
                    onClick = { onThicknessSelected(option) }
                )
            }
        }
    }
}

@Composable
private fun ThicknessChip(
    option: TileThicknessOptionUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        if (option.isSelected) Color(0xFF2D3142) else Color.White
    )
    val borderColor by animateColorAsState(
        if (option.isSelected) Color(0xFF2D3142) else Color(0xFF2D3142)
    )
    val textColor by animateColorAsState(
        if (option.isSelected) Color(0xFFC53030) else Color(0xFF2D3142)
    )

    Text(
        text = option.label,
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (option.isSelected) FontWeight.Bold else FontWeight.Medium,
        color = textColor
    )
}
