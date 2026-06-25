package com.example.arplitka.features.tiledetails.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import arplitka.features.tile_details.generated.resources.Res
import arplitka.features.tile_details.generated.resources.category
import arplitka.features.tile_details.generated.resources.characteristics
import arplitka.features.tile_details.generated.resources.in_stock
import arplitka.features.tile_details.generated.resources.in_stock_format
import arplitka.features.tile_details.generated.resources.material
import arplitka.features.tile_details.generated.resources.size
import arplitka.features.tile_details.generated.resources.size_format
import arplitka.features.tile_details.generated.resources.surface_type
import arplitka.features.tile_details.generated.resources.thickness
import arplitka.features.tile_details.generated.resources.thickness_format
import arplitka.features.tile_details.generated.resources.tiles_per_box
import arplitka.features.tile_details.generated.resources.tiles_per_box_format
import com.example.arplitka.features.tiledetails.presentation.mapper.categoryDisplayText
import com.example.arplitka.shared.ui.core.mapper.toDisplayStringResource
import com.example.arplitka.features.tiledetails.presentation.screen.components.CharacteristicRow
import com.example.arplitka.features.tiledetails.presentation.screen.components.TileColorSwatches
import com.example.arplitka.features.tiledetails.presentation.screen.components.TileDetailsPriceRow
import com.example.arplitka.shared.tiles.domain.model.Tile
import com.example.arplitka.shared.ui.kit.components.TileImage
import com.example.arplitka.shared.ui.kit.utils.resolveTileImageUrl
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TileDetailsInfo(
    tile: Tile,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp)
    ) {
        val imageUrl = remember(tile.id, tile.photos.firstOrNull()) {
            resolveTileImageUrl(tile.photos.firstOrNull().orEmpty())
        }

        TileImage(
            imageUrl = imageUrl,
            contentDescription = tile.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.2f)
                .background(Color(0xFFF5F5F5))
        )

        Column(modifier = Modifier.padding(20.dp)) {
            TileDetailsHeader(tile = tile)
            TileDetailsCharacteristics(tile = tile)
            TileColorSwatches(colors = tile.colors)
        }
    }
}

@Composable
private fun TileDetailsHeader(tile: Tile) {
    Text(
        text = tile.name,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF2D3142)
    )

    TileDetailsPriceRow(
        basePrice = tile.basePrice,
        unit = tile.unit
    )

    Text(
        text = tile.manufacturer,
        style = MaterialTheme.typography.bodyLarge,
        color = Color.Gray,
        modifier = Modifier.padding(top = 8.dp)
    )

    Text(
        text = tile.description,
        style = MaterialTheme.typography.bodyMedium,
        color = Color(0xFF4A4E69),
        lineHeight = 22.sp,
        modifier = Modifier.padding(top = 16.dp)
    )
}

@Composable
private fun TileDetailsCharacteristics(tile: Tile) {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 24.dp),
        color = Color(0xFFF0F0F0)
    )

    Text(
        text = stringResource(Res.string.characteristics),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF2D3142)
    )

    CharacteristicRow(
        label = stringResource(Res.string.material),
        value = tile.material
    )
    CharacteristicRow(
        label = stringResource(Res.string.surface_type),
        value = tile.surfaceType
    )
    CharacteristicRow(
        label = stringResource(Res.string.category),
        value = categoryDisplayText(tile.category)
    )

    tile.variants.firstOrNull()?.let { variant ->
        CharacteristicRow(
            label = stringResource(Res.string.size),
            value = stringResource(
                Res.string.size_format,
                variant.widthMm,
                variant.heightMm
            )
        )
        CharacteristicRow(
            label = stringResource(Res.string.thickness),
            value = stringResource(Res.string.thickness_format, variant.thicknessMm)
        )
        variant.tilesPerBox?.let { tilesPerBox ->
            CharacteristicRow(
                label = stringResource(Res.string.tiles_per_box),
                value = stringResource(Res.string.tiles_per_box_format, tilesPerBox)
            )
        }
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
