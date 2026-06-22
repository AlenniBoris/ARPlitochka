package com.example.arplitka.features.catalog.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.arplitka.mock.core.AssetReader
import com.example.arplitka.shared.tiles.domain.model.Tile
import com.example.arplitka.shared.tiles.domain.validation.atomic.url.isRemoteImageUrl

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
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tiles, key = { it.id }) { tile ->
            TileCard(tile = tile)
        }
    }
}

@Composable
private fun TileCard(tile: Tile) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Выбор плитки */ }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imageUrl = remember(tile.id, tile.photos.firstOrNull()) {
                resolveTilePreviewUrl(tile.photos.firstOrNull().orEmpty())
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.LightGray, MaterialTheme.shapes.small)
                    .border(1.dp, Color.LightGray, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center
            ) {
                TilePreviewImage(
                    imageUrl = imageUrl,
                    contentDescription = tile.name,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = tile.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = tile.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Цветов: ${tile.colors.size}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "Цена от: ${tile.basePrice} ₽",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

internal fun resolveTilePreviewUrl(rawUrl: String): String {
    if (rawUrl.isBlank()) return rawUrl
    if (isRemoteImageUrl(rawUrl)) return rawUrl

    val cleanPath = rawUrl.removePrefix("file:///android_asset/").removePrefix("/")
    return AssetReader.resolveAssetPath(cleanPath) ?: rawUrl
}
