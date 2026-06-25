package com.example.arplitka.features.catalog.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arplitka.shared.tiles.domain.model.Tile
import com.example.arplitka.shared.ui.core.format.atomic.formatPrice
import com.example.arplitka.shared.ui.core.mapper.toDisplayStringResource
import com.example.arplitka.shared.ui.kit.components.TileImage
import com.example.arplitka.shared.ui.kit.utils.resolveTileImageUrl
import org.jetbrains.compose.resources.stringResource
import arplitka.shared.ui.core.generated.resources.Res as SharedRes
import arplitka.shared.ui.core.generated.resources.price_format

@Composable
internal fun CatalogList(
    tiles: List<Tile>,
    onTileClick: (Long) -> Unit,
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
                    .clickable { onTileClick(tile.id) }
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
            resolveTileImageUrl(tile.photos.firstOrNull().orEmpty())
        }

        TileImage(
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
                stringResource(tile.unit.toDisplayStringResource())
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
