package com.example.arplitka.features.floordetection.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.example.arplitka.shared.ar.domain.model.FloorWorkflowStage

@Composable
fun ArActionButtons(
    uiState: FloorUiState,
    onAddPoint: () -> Unit,
    onUndoPoint: () -> Unit,
    onConfirmContour: () -> Unit,
    onToggleTileVisibility: () -> Unit,
    onChangeTileType: () -> Unit,
    onRotateTexture: () -> Unit,
    onClearSection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canClearSection = uiState.points.isNotEmpty() || 
            uiState.stage.ordinal >= FloorWorkflowStage.CONTOUR_CONFIRMED.ordinal ||
            uiState.stage == FloorWorkflowStage.PLACEMENT_EMPTY ||
            uiState.stage == FloorWorkflowStage.SEARCHING_FLOOR ||
            uiState.stage == FloorWorkflowStage.INITIALIZING

    Box(modifier = modifier.padding(bottom = 120.dp)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (uiState.stage) {
                FloorWorkflowStage.INITIALIZING,
                FloorWorkflowStage.SEARCHING_FLOOR -> {
                    // No buttons during search
                }

                FloorWorkflowStage.PLACEMENT_EMPTY,
                FloorWorkflowStage.PLACEMENT_ACTIVE -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.stage == FloorWorkflowStage.PLACEMENT_ACTIVE) {
                            UndoButton(onClick = onUndoPoint)
                        }
                        AddPointButton(
                            onClick = onAddPoint,
                            enabled = uiState.hasCenterHit
                        )
                    }
                }

                FloorWorkflowStage.CONTOUR_CLOSED -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UndoButton(onClick = onUndoPoint)
                        ConfirmButton(onClick = onConfirmContour)
                    }
                }

                FloorWorkflowStage.CONTOUR_CONFIRMED -> {
                    Button(
                        onClick = onToggleTileVisibility,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(text = stringResource(R.string.btn_add_tile))
                    }
                }

                FloorWorkflowStage.TILE_LAYOUT -> {
                    TileLayoutControls(
                        uiState = uiState,
                        onToggleTileVisibility = onToggleTileVisibility,
                        onChangeTileType = onChangeTileType,
                        onRotateTexture = onRotateTexture
                    )
                }
            }

            if (canClearSection) {
                ClearSectionButton(onClick = onClearSection)
            }
        }
    }
}

@Composable
private fun AddPointButton(onClick: () -> Unit, enabled: Boolean) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(32.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black,
            disabledContainerColor = Color.White.copy(alpha = 0.3f)
        ),
        modifier = Modifier.size(72.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(R.string.btn_add_point),
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun ConfirmButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(32.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF4CAF50),
            contentColor = Color.White
        ),
        modifier = Modifier.size(80.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = stringResource(R.string.btn_ok),
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun TileLayoutControls(
    uiState: FloorUiState,
    onToggleTileVisibility: () -> Unit,
    onChangeTileType: () -> Unit,
    onRotateTexture: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            uiState.textureRotation.ordinal * 45
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
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onToggleTileVisibility,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black.copy(alpha = 0.72f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(text = stringResource(R.string.btn_remove_tile))
            }

            Button(
                onClick = onChangeTileType,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.92f),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(text = stringResource(R.string.btn_change_tile))
            }
        }
    }
}

@Composable
private fun ClearSectionButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(32.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.85f),
            contentColor = Color.Black
        ),
        modifier = Modifier.size(56.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = stringResource(R.string.btn_clear_section)
        )
    }
}

@Composable
private fun UndoButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
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
