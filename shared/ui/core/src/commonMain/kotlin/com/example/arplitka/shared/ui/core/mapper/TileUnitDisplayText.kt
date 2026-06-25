package com.example.arplitka.shared.ui.core.mapper

import arplitka.shared.ui.core.generated.resources.Res
import arplitka.shared.ui.core.generated.resources.unit_box
import arplitka.shared.ui.core.generated.resources.unit_m2
import arplitka.shared.ui.core.generated.resources.unit_piece
import com.example.arplitka.shared.tiles.domain.model.TileUnit
import org.jetbrains.compose.resources.StringResource

fun TileUnit.toDisplayStringResource(): StringResource = when (this) {
    TileUnit.M2 -> Res.string.unit_m2
    TileUnit.PIECE -> Res.string.unit_piece
    TileUnit.BOX -> Res.string.unit_box
}
