package com.example.arplitka.features.tiledetails.presentation.mapper

import androidx.compose.runtime.Composable
import arplitka.features.tile_details.generated.resources.Res
import arplitka.features.tile_details.generated.resources.category_clinker
import arplitka.features.tile_details.generated.resources.category_facade
import arplitka.features.tile_details.generated.resources.category_paving_stones
import arplitka.features.tile_details.generated.resources.category_paving_stones_v2
import org.jetbrains.compose.resources.stringResource

@Composable
fun categoryDisplayText(category: String): String = when (category) {
    "paving_stones" -> stringResource(Res.string.category_paving_stones)
    "paving_stones_v2" -> stringResource(Res.string.category_paving_stones_v2)
    "clinker" -> stringResource(Res.string.category_clinker)
    "facade" -> stringResource(Res.string.category_facade)
    else -> category
}
