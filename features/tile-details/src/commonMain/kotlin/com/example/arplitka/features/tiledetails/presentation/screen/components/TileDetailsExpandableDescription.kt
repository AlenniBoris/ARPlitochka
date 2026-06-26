package com.example.arplitka.features.tiledetails.presentation.screen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import arplitka.features.tile_details.generated.resources.Res
import arplitka.features.tile_details.generated.resources.less
import arplitka.features.tile_details.generated.resources.more
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TileDetailsExpandableDescription(
    description: String,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    var hasOverflow by remember(description) { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF4A4E69),
            lineHeight = 22.sp,
            maxLines = if (isExpanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { textLayoutResult ->
                if (!isExpanded) {
                    hasOverflow = textLayoutResult.hasVisualOverflow
                }
            }
        )

        if (hasOverflow || isExpanded) {
            Text(
                text = stringResource(if (isExpanded) Res.string.less else Res.string.more),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFC53030),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable(onClick = onToggleExpanded)
            )
        }
    }
}
