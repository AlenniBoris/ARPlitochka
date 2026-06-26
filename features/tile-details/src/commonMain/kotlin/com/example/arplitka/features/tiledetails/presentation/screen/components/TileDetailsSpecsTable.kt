package com.example.arplitka.features.tiledetails.presentation.screen.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import arplitka.features.tile_details.generated.resources.Res
import arplitka.features.tile_details.generated.resources.abrasion_class
import arplitka.features.tile_details.generated.resources.characteristics
import arplitka.features.tile_details.generated.resources.color
import arplitka.features.tile_details.generated.resources.concrete_class
import arplitka.features.tile_details.generated.resources.element_size_format
import arplitka.features.tile_details.generated.resources.element_sizes
import arplitka.features.tile_details.generated.resources.features
import arplitka.features.tile_details.generated.resources.frost_resistance
import arplitka.features.tile_details.generated.resources.in_stock
import arplitka.features.tile_details.generated.resources.in_stock_format
import arplitka.features.tile_details.generated.resources.m2_per_pallet
import arplitka.features.tile_details.generated.resources.material
import arplitka.features.tile_details.generated.resources.surface_type
import arplitka.features.tile_details.generated.resources.thickness
import arplitka.features.tile_details.generated.resources.thickness_format
import arplitka.features.tile_details.generated.resources.tiles_per_box
import arplitka.features.tile_details.generated.resources.tiles_per_box_format
import arplitka.features.tile_details.generated.resources.usage
import arplitka.features.tile_details.generated.resources.water_absorption
import arplitka.features.tile_details.generated.resources.weight_per_m2
import com.example.arplitka.features.tiledetails.presentation.mapper.tileFeaturesDisplayText
import com.example.arplitka.features.tiledetails.presentation.mapper.tileUsageWaysDisplayText
import com.example.arplitka.features.tiledetails.presentation.model.TileColorOptionUi
import com.example.arplitka.features.tiledetails.presentation.model.TileThicknessOptionUi
import com.example.arplitka.shared.tiles.domain.model.Tile
import com.example.arplitka.shared.tiles.domain.model.TileElementSize
import com.example.arplitka.shared.tiles.domain.model.TileVariant
import com.example.arplitka.shared.ui.core.mapper.toDisplayStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TileDetailsSpecsTable(
    tile: Tile,
    selectedColor: TileColorOptionUi,
    selectedThickness: TileThicknessOptionUi,
    selectedVariant: TileVariant?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = Color(0xFFF0F0F0))

        Text(
            text = stringResource(Res.string.characteristics),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFC53030),
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
        )

        CharacteristicRow(
            label = stringResource(Res.string.thickness),
            value = stringResource(Res.string.thickness_format, selectedThickness.thicknessMm)
        )
        CharacteristicRow(
            label = stringResource(Res.string.color),
            value = selectedColor.name
        )

        val elementSizes = selectedVariant?.elementSizes.orEmpty()
        if (elementSizes.isNotEmpty()) {
            ElementSizesRow(sizes = elementSizes)
        }

        tile.concreteClass?.let {
            CharacteristicRow(label = stringResource(Res.string.concrete_class), value = it)
        }
        tile.frostResistance?.let {
            CharacteristicRow(label = stringResource(Res.string.frost_resistance), value = it)
        }
        tile.waterAbsorptionPercent?.let {
            CharacteristicRow(label = stringResource(Res.string.water_absorption), value = it)
        }
        tile.abrasionClass?.let {
            CharacteristicRow(label = stringResource(Res.string.abrasion_class), value = it)
        }

        selectedVariant?.weightKgPerM2?.let {
            CharacteristicRow(label = stringResource(Res.string.weight_per_m2), value = it.toString())
        }
        selectedVariant?.m2PerPallet?.let {
            CharacteristicRow(label = stringResource(Res.string.m2_per_pallet), value = it.toString())
        }

        val featuresText = tileFeaturesDisplayText(tile.features)
        if (featuresText.isNotBlank()) {
            CharacteristicRow(label = stringResource(Res.string.features), value = featuresText)
        }

        val usageText = tileUsageWaysDisplayText(tile.usageWays)
        if (usageText.isNotBlank()) {
            CharacteristicRow(label = stringResource(Res.string.usage), value = usageText)
        }

        CharacteristicRow(label = stringResource(Res.string.material), value = tile.material)
        CharacteristicRow(label = stringResource(Res.string.surface_type), value = tile.surfaceType)

        selectedVariant?.tilesPerBox?.let { tilesPerBox ->
            CharacteristicRow(
                label = stringResource(Res.string.tiles_per_box),
                value = stringResource(Res.string.tiles_per_box_format, tilesPerBox)
            )
        }

        selectedVariant?.let { variant ->
            CharacteristicRow(
                label = stringResource(Res.string.in_stock),
                value = stringResource(
                    Res.string.in_stock_format,
                    variant.stockCount,
                    stringResource(tile.unit.toDisplayStringResource())
                )
            )
        }
    }
}

@Composable
private fun ElementSizesRow(sizes: List<TileElementSize>) {
    val sizesText = sizes.map { size ->
        stringResource(
            Res.string.element_size_format,
            size.widthMm,
            size.heightMm
        )
    }.joinToString(", ")

    CharacteristicRow(
        label = stringResource(Res.string.element_sizes),
        value = sizesText
    )
}
