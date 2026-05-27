package com.example.arplitka.features.floordetection.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.arplitka.features.floordetection.R
import com.example.arplitka.features.floordetection.domain.model.FloorUiState

@Composable
fun ArActionButtons(
    uiState: FloorUiState,
    onAddPoint: () -> Unit,
    onUndoPoint: () -> Unit,
    onRotateTexture: () -> Unit,
    onToggleTile: () -> Unit,
    onClearSection: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!uiState.isFinalized) {
        Box(
            modifier = modifier
                .padding(bottom = 120.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Undo Button
                if (uiState.points.isNotEmpty()) {
                    Button(
                        onClick = onUndoPoint,
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black.copy(alpha = 0.6f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.size(56.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.btn_undo)
                        )
                    }
                }

                // Add Point / OK Button
                Button(
                    onClick = onAddPoint,
                    enabled = uiState.hasCenterHit,
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isPolygonClosed) Color(0xFF4CAF50) else Color.White,
                        contentColor = if (uiState.isPolygonClosed) Color.White else Color.Black,
                        disabledContainerColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.size(if (uiState.isPolygonClosed) 80.dp else 72.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isPolygonClosed) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = if (uiState.isPolygonClosed) stringResource(R.string.btn_ok) else stringResource(R.string.btn_add_point),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    } else {
        Box(
            modifier = modifier
                .padding(horizontal = 16.dp, vertical = 36.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${stringResource(R.string.texture_rotation_title)}: ${
                                stringResource(
                                    R.string.texture_rotation_format,
                                    uiState.textureRotation.ordinal * 45 // Simplified for now, or use a mapper
                                )
                            }",
                            color = Color.White
                        )
                        Button(
                            onClick = onRotateTexture,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            )
                        ) {
                            Text(text = stringResource(R.string.btn_rotate_texture))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onToggleTile,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.8f),
                            contentColor = Color.Black
                        )
                    ) {
                        Text(text = stringResource(R.string.btn_toggle_tile))
                    }
                }

                Button(
                    onClick = onClearSection,
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.size(72.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.btn_clear_section),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
