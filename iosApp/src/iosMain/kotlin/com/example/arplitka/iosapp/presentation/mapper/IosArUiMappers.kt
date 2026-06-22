package com.example.arplitka.iosapp.presentation.mapper

import com.example.arplitka.iosapp.platform.render.MIN_FLOOR_AREA_M2
import com.example.arplitka.shared.ar.contracts.model.ArInstruction
import com.example.arplitka.shared.ar.contracts.model.ArTrackingStatus
import com.example.arplitka.shared.ar.domain.logic.AddPointRejectReason
import com.example.arplitka.shared.ar.domain.model.FloorContourUiState
import com.example.arplitka.shared.ui.kit.ar.CenterReticleState
import kotlin.math.roundToInt

internal fun ArTrackingStatus.toIosText(): String = when (this) {
    ArTrackingStatus.INITIALIZING -> "Инициализация…"
    ArTrackingStatus.SEARCHING_FLOOR -> "Наведите прицел на поверхность"
    ArTrackingStatus.FLOOR_DETECTED -> "Поверхность под прицелом"
    ArTrackingStatus.TRACKING_LOST -> "Трекинг потерян"
    ArTrackingStatus.POLYGON_CLOSED -> "Контур замкнут"
    ArTrackingStatus.FINALIZED -> ""
}

internal fun AddPointRejectReason.toIosHint(): String = when (this) {
    AddPointRejectReason.FINALIZED -> "Разметка уже завершена"
    AddPointRejectReason.POLYGON_CLOSED -> "Сначала подтвердите контур"
    AddPointRejectReason.SNAP_ACTIVE -> "Отведите прицел от точки"
    AddPointRejectReason.NO_HIT -> "Наведите прицел на поверхность"
    AddPointRejectReason.TOO_CLOSE_TO_LAST -> "Отойдите дальше от предыдущей точки"
    AddPointRejectReason.HEIGHT_OUT_OF_RANGE -> "Точка слишком высоко или низко относительно контура"
}

internal fun FloorContourUiState.toStatusDetailText(): String? {
    val formatArea = { value: Float ->
        "${(value * 100).roundToInt() / 100.0} m²"
    }
    return when {
        isFloorDetected && selectedArea > 0f ->
            "Под прицелом: ${formatArea(selectedArea)}"
        largestPlaneAreaM2 >= MIN_FLOOR_AREA_M2 ->
            "Крупнейшая поверхность: ${formatArea(largestPlaneAreaM2)}"
        else -> null
    }
}

internal fun FloorContourUiState.toReticleState(
    placementHint: String?,
    placementStatus: String,
    isPlacementPlaceable: Boolean
): CenterReticleState = when {
    isPolygonClosed -> CenterReticleState.CLOSED
    !showContourActions || !hasCenterHit -> CenterReticleState.INACTIVE
    snappedPointIndex != null -> CenterReticleState.SNAP
    placementStatus == "height" -> CenterReticleState.INVALID
    placementStatus == "stale" -> CenterReticleState.INACTIVE
    placementStatus == "no-hit" -> CenterReticleState.INACTIVE
    !isPlacementPlaceable && placementStatus in setOf("scan-valid", "valid", "preview") ->
        CenterReticleState.INVALID
    placementStatus == "preview" -> CenterReticleState.VALID
    placementHint != null -> CenterReticleState.INVALID
    else -> CenterReticleState.VALID
}

internal fun ArInstruction.toIosText(): String = when (this) {
    ArInstruction.PLEASE_WAIT -> "Пожалуйста, подождите"
    ArInstruction.SEARCHING -> "Наведите прицел на нужную поверхность"
    ArInstruction.SURFACE_NEARBY -> "Сетка найдена — наведите прицел на неё"
    ArInstruction.MOVE_PHONE -> "Медленно наведите камеру на поверхность"
    ArInstruction.DETECTED -> "Точки показывают поверхность под прицелом"
    ArInstruction.CONTOUR_CLOSED -> "Нажмите OK, чтобы завершить разметку"
    ArInstruction.CONTOUR_CONFIRMED -> "Нажмите «Добавить плитку» для предпросмотра"
    ArInstruction.TILE_VISIBLE -> "Плитка наложена. Можно повернуть или сменить"
    ArInstruction.EMPTY -> ""
}

