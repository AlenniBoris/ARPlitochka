# Стабильность Контура В AR

Этот документ — канонический контракт для точек контура, стабильности полигона и ручного выравнивания.

## Цель

Поставленные точки контура должны визуально оставаться на физической поверхности. Небольшой drift AR-трекинга можно корректировать автоматически, но крупные сдвиги должны применяться только явно через действие «Выровнять контур».

## Shared State

Референсные файлы:

- `shared/ar/domain/.../FloorArController.kt`
- `shared/ar/domain/.../FloorContourUiState.kt`
- `shared/ar/contracts/.../FloorArContracts.kt`

Важные state-поля:

- `placedPoints`
- `isPolygonClosed`
- `isFinalized`
- `isTileVisible`
- `showContourPoints`
- `showContourLines`
- `showSectionFill`
- `showTileControls`

Правила:

- Точки видимы, пока есть поставленные точки и выключен tile mode.
- Линии видимы при минимум двух точках и выключенном tile mode.
- Заливка зоны видима, когда полигон замкнут и содержит минимум три точки.
- Tile mode скрывает точки и линии, но сохраняет заливку зоны.

## Android

Android использует ARCore anchors из hit'ов постановки точек. Android AR-flow пока реализован через legacy-слой `FloorUiState` / `FloorArViewModel`, но его phase-derived visibility выровнен с shared/iOS поведением.

Референсные файлы:

- `features/floor-detection/.../FloorArViewModel.kt`
- `features/floor-detection/.../FloorModels.kt`
- `features/floor-detection/.../ArSceneLayer.kt`

## iOS

iOS использует root-anchor модель для стабильности контура.

Референсные файлы:

- `iosApp/.../IosFloorAnchorStore.kt`
- `iosApp/.../IosArSessionCoordinator.kt`
- `iosApp/.../IosArPlacementRules.kt`
- `iosApp/.../IosArContourRenderer.kt`

Модель хранения:

- Первая точка создаёт contour root anchor.
- Каждая точка контура хранит local X/Z относительно этого root anchor.
- Renderer получает спроецированные world positions из `FloorArController.currentState()`.
- Fallback world positions используются, когда root anchor невозможно разрешить.

## Политика Коррекций На iOS

Отслеживаются две дельты:

- `r:` drift origin root-anchor.
- `d:` максимальная display delta между candidate positions и текущими отображаемыми positions.

Эффективная correction delta — `max(r, d)`.

| Уровень | Порог | Поведение |
|---------|-------|-----------|
| Micro | `<= 3 cm` | Применить сразу при стабильном трекинге. |
| Small | `3-8 cm` | Автоматически применить только после tracking instability и нескольких стабильных кадров. |
| Macro | `>= 8 cm` | Не применять автоматически; предложить «Выровнять контур». |

Точные пороги находятся в `IosArPlacementRules.kt`.

## Кнопка Realign

«Выровнять контур» должна появляться только тогда, когда контур мог сместиться настолько, что явное действие пользователя безопаснее автоматической коррекции.

Ожидаемое поведение:

- Кнопка появляется над contour/tile controls.
- Кнопка не блокирует rescan.
- Действие применяет актуальный root-anchor transform из текущего `ARFrame`.
- После успешной коррекции manual realign latch очищается.

## Известные Риски

- ARKit может relocalize и сдвинуть world space после потери трекинга.
- Частые автоматические коррекции могут выглядеть как скачки точек.
- Слишком строгие пороги могут оставить контур визуально смещённым, пока пользователь не выполнит realign.

## Связанные Исторические Документы

- `docs/archive/completed-plans/ios-ar-point-stability.md`
- `docs/investigations/ios-ar-point-stability-task.md`
- `docs/investigations/android-contour-visibility.md`
