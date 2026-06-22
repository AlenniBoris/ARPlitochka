package com.example.arplitka.features.catalog.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arplitka.mock.core.AssetReader
import com.example.arplitka.shared.tiles.domain.model.Tile
import com.example.arplitka.shared.tiles.domain.model.TileUnit
import com.example.arplitka.shared.tiles.domain.validation.atomic.url.isRemoteImageUrl
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import arplitka.shared.ui.core.generated.resources.Res as SharedRes
import arplitka.shared.ui.core.generated.resources.price_format
import arplitka.shared.ui.core.generated.resources.unit_m2
import arplitka.shared.ui.core.generated.resources.unit_piece
import arplitka.shared.ui.core.generated.resources.unit_box

@Composable
internal expect fun TilePreviewImage(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier
)

@Composable
internal fun CatalogList(
    tiles: List<Tile>,
    modifier: Modifier = Modifier
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(15.dp),
        verticalItemSpacing = 15.dp
    ) {
        items(tiles, key = { it.id }) { tile ->
            TileCard(
                tile = tile,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 10.dp, shape = RoundedCornerShape(24.dp))
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .clickable { /* Выбор плитки */ }
                    .padding(10.dp)
            )
        }
    }
}

@Composable
private fun TileCard(
    tile: Tile,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        val imageUrl = remember(tile.id, tile.photos.firstOrNull()) {
            resolveTilePreviewUrl(tile.photos.firstOrNull().orEmpty())
        }

        TilePreviewImage(
            imageUrl = imageUrl,
            contentDescription = tile.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFF5F5F5))
        )

        Text(
            text = tile.name,
            modifier = Modifier.padding(top = 15.dp),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D3142),
                fontSize = 16.sp
            ),
            maxLines = 1
        )

        Text(
            text = stringResource(
                SharedRes.string.price_format,
                formatPrice(tile.basePrice),
                stringResource(tile.unit.toStringResource())
            ),
            modifier = Modifier.padding(top = 20.dp),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC53030),
                fontSize = 14.sp
            )
        )
    }
}

private fun formatPrice(price: Double): String {
    return if (price == price.toLong().toDouble()) {
        price.toLong().toString()
    } else {
        price.toString()
    }
}

private fun TileUnit.toStringResource(): StringResource = when (this) {
    TileUnit.M2 -> SharedRes.string.unit_m2
    TileUnit.PIECE -> SharedRes.string.unit_piece
    TileUnit.BOX -> SharedRes.string.unit_box
}

internal fun resolveTilePreviewUrl(rawUrl: String): String {
    if (rawUrl.isBlank()) return rawUrl
    if (isRemoteImageUrl(rawUrl)) return rawUrl

    val cleanPath = rawUrl.removePrefix("file:///android_asset/").removePrefix("/")
    return AssetReader.resolveAssetPath(cleanPath) ?: rawUrl
}
