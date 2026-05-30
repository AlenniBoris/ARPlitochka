package com.example.arplitka.shared.ar.contracts.state

import com.example.arplitka.shared.ar.contracts.model.ArPoint2D
import com.example.arplitka.shared.ar.contracts.model.ArPoint3D
import com.example.arplitka.shared.ar.contracts.model.ArSurfacePolygon
import com.example.arplitka.shared.ar.contracts.model.ArTileTexture
import com.example.arplitka.shared.ar.contracts.model.ArTrackingStatus

data class SharedFloorArState(
    val trackingStatus: ArTrackingStatus = ArTrackingStatus.INITIALIZING,
    val placedPoints: List<ArPoint3D> = emptyList(),
    val normalizedPolygon: ArSurfacePolygon = ArSurfacePolygon(emptyList()),
    val selectedTexture: ArTileTexture? = null,
    val isFinalized: Boolean = false
)

sealed interface FloorArEvent {
    data object AddPoint : FloorArEvent
    data object UndoPoint : FloorArEvent
    data object Reset : FloorArEvent
    data object FinalizeArea : FloorArEvent
    data object RotateTexture : FloorArEvent
    data class PlatformPointUpdated(val point: ArPoint3D?) : FloorArEvent
}

sealed interface FloorArCommand {
    data object StartSession : FloorArCommand
    data object PauseSession : FloorArCommand
    data object ResetSession : FloorArCommand
    data class RenderPolygon(val polygon: ArSurfacePolygon, val texture: ArTileTexture?) : FloorArCommand
}

fun List<ArPoint3D>.normalizeTo2D(): ArSurfacePolygon {
    val first = firstOrNull() ?: return ArSurfacePolygon(emptyList())
    return ArSurfacePolygon(
        points = map { point ->
            ArPoint2D(
                xMeters = point.xMeters - first.xMeters,
                yMeters = point.zMeters - first.zMeters
            )
        }
    )
}
