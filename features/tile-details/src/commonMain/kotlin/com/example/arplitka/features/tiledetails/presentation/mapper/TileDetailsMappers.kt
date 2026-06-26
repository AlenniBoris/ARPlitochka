package com.example.arplitka.features.tiledetails.presentation.mapper

import androidx.compose.runtime.Composable
import arplitka.features.tile_details.generated.resources.Res
import arplitka.features.tile_details.generated.resources.category_clinker
import arplitka.features.tile_details.generated.resources.category_facade
import arplitka.features.tile_details.generated.resources.category_paving_stones
import arplitka.features.tile_details.generated.resources.category_paving_stones_v2
import arplitka.features.tile_details.generated.resources.feature_anti_slip
import arplitka.features.tile_details.generated.resources.feature_bevel
import arplitka.features.tile_details.generated.resources.feature_color_mix
import arplitka.features.tile_details.generated.resources.feature_frost_resistant
import arplitka.features.tile_details.generated.resources.feature_micro_bevel
import arplitka.features.tile_details.generated.resources.feature_textured_surface
import arplitka.features.tile_details.generated.resources.features_list_separator
import arplitka.features.tile_details.generated.resources.usage_driveway
import arplitka.features.tile_details.generated.resources.usage_home_and_garden
import arplitka.features.tile_details.generated.resources.usage_list_separator
import arplitka.features.tile_details.generated.resources.usage_pedestrian_area
import arplitka.features.tile_details.generated.resources.usage_public_space
import com.example.arplitka.shared.tiles.domain.model.TileFeature
import com.example.arplitka.shared.tiles.domain.model.TileUsageWay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun categoryDisplayText(category: String): String = when (category) {
    "paving_stones" -> stringResource(Res.string.category_paving_stones)
    "paving_stones_v2" -> stringResource(Res.string.category_paving_stones_v2)
    "clinker" -> stringResource(Res.string.category_clinker)
    "facade" -> stringResource(Res.string.category_facade)
    else -> category
}

@Composable
fun tileUsageWayDisplayText(usageWay: TileUsageWay): String = stringResource(usageWay.toStringResource())

@Composable
fun tileFeatureDisplayText(feature: TileFeature): String = stringResource(feature.toStringResource())

@Composable
fun tileUsageWaysDisplayText(usageWays: List<TileUsageWay>): String {
    if (usageWays.isEmpty()) return ""
    val separator = stringResource(Res.string.usage_list_separator)
    return usageWays.map { tileUsageWayDisplayText(it) }.joinToString(separator)
}

@Composable
fun tileFeaturesDisplayText(features: List<TileFeature>): String {
    if (features.isEmpty()) return ""
    val separator = stringResource(Res.string.features_list_separator)
    return features.map { tileFeatureDisplayText(it) }.joinToString(separator)
}

private fun TileUsageWay.toStringResource(): StringResource = when (this) {
    TileUsageWay.HOME_AND_GARDEN -> Res.string.usage_home_and_garden
    TileUsageWay.PUBLIC_SPACE -> Res.string.usage_public_space
    TileUsageWay.DRIVEWAY -> Res.string.usage_driveway
    TileUsageWay.PEDESTRIAN_AREA -> Res.string.usage_pedestrian_area
}

private fun TileFeature.toStringResource(): StringResource = when (this) {
    TileFeature.MICRO_BEVEL -> Res.string.feature_micro_bevel
    TileFeature.BEVEL -> Res.string.feature_bevel
    TileFeature.ANTI_SLIP -> Res.string.feature_anti_slip
    TileFeature.FROST_RESISTANT -> Res.string.feature_frost_resistant
    TileFeature.COLOR_MIX -> Res.string.feature_color_mix
    TileFeature.TEXTURED_SURFACE -> Res.string.feature_textured_surface
}
