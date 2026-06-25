package com.example.arplitka.features.tiledetails.presentation.screen.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import arplitka.features.tile_details.generated.resources.Res
import arplitka.features.tile_details.generated.resources.price_unit_suffix
import arplitka.shared.ui.core.generated.resources.Res as SharedRes
import arplitka.shared.ui.core.generated.resources.price_amount
import com.example.arplitka.shared.ui.core.format.atomic.formatPrice
import com.example.arplitka.shared.ui.core.mapper.toDisplayStringResource
import com.example.arplitka.shared.tiles.domain.model.TileUnit
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TileDetailsPriceRow(
    basePrice: Double,
    unit: TileUnit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = stringResource(
                SharedRes.string.price_amount,
                formatPrice(basePrice)
            ),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFC53030)
        )
        Text(
            text = stringResource(
                Res.string.price_unit_suffix,
                stringResource(unit.toDisplayStringResource())
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )
    }
}
