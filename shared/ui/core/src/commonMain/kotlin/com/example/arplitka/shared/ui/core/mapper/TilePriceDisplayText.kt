package com.example.arplitka.shared.ui.core.mapper

import androidx.compose.runtime.Composable
import arplitka.shared.ui.core.generated.resources.Res
import arplitka.shared.ui.core.generated.resources.price_format
import com.example.arplitka.shared.tiles.domain.model.TileUnit
import com.example.arplitka.shared.ui.core.format.atomic.formatPrice
import org.jetbrains.compose.resources.stringResource

@Composable
fun formatTilePriceLabel(basePrice: Double, unit: TileUnit): String =
    stringResource(
        Res.string.price_format,
        formatPrice(basePrice),
        stringResource(unit.toDisplayStringResource())
    )
