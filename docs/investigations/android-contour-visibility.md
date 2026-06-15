# Android: видимость контура — анализ и исправления

> Исторический analysis-док по конкретному исправлению Android visibility. Канонический контракт visibility и стабильности находится в [`AR/CONTOUR_STABILITY.md`](../ar/PLATFORM_CONTOUR_STABILITY.md), tile behavior — в [`AR/TILE_FILLING.md`](../ar/PLATFORM_TILE_FILLING.md).

Документ фиксирует результаты плана **Анализ Логики Контура Android** (июнь 2026).

## Контекст

После доработок iOS/shared контракт `FloorContourUiState` показывает:
- зелёные точки — пока есть точки и не включена плитка;
- синие линии — при ≥2 точках и без плитки;
- синюю заливку — сразу при `Closed: Yes` (≥3 точек), ещё до финализации.

Android использует legacy-модель `FloorUiState` в `features/floor-detection`. До исправления код расходился с документацией и shared-контрактом.

## Сопоставление контрактов

| Флаг | Shared `FloorContourUiState` | Android `FloorUiState` (до) | Android `FloorUiState` (после) |
|------|------------------------------|-----------------------------|--------------------------------|
| `showContourPoints` | `placedPoints.isNotEmpty() && !isTileVisible` | `!isContourConfirmed` | `points.isNotEmpty() && !isTileVisible` |
| `showContourLines` | `size >= 2 && !isTileVisible` | то же | то же |
| `showSectionFill` | `isPolygonClosed && size >= 3` | только после `confirmContour` | `isPolygonClosed && size >= 3` |
| `showPlaneRenderer` | `!isFinalized` | `!isContourConfirmed` | `!isContourConfirmed` (без изменений) |
| Финализация | `isFinalized` | `isContourConfirmed` | `isContourConfirmed` (эквивалент) |

## Симптом 1: при расстановке видны точки и плашки, но не синие линии

### Наблюдение

Distance labels и contour lines рендерятся в одном блоке `if (uiState.showContourLines)` в `ArSceneLayer.kt`. Если labels видны, state-флаг `showContourLines == true` и `createSegmentGeometry(...)` возвращает не-null сегмент.

### Корневая причина

Проблема **не в state**, а в **визуализации** `CubeNode`:
- тонкий вертикальный `CubeNode` (2–6 мм) терялся в depth buffer ARCore `planeRenderer` и полупрозрачного `ShapeNode` fill;
- distance labels (`ImageNode`, плоские полоски на +18 мм) и зелёные точки оставались видимыми;
- при `Show lines: Yes` сегменты создавались, но `CubeNode` не проходил depth-test.

Мерцание при расстановке: recomposition каждый AR-кадр + пересоздание nodes без `key()`; при snap/close к первой точке кратковременно меняется геометрия сегмента.

### Исправление

Контурные рёбра переведены на **плоские `ImageNode`-полоски** (тот же путь, что у distance labels):
- `createLineStripBitmap()` / `ContourLineStripNode` в `ArSceneLayer.kt`;
- фиксированный `LINE_VISUAL_OFFSET_M = 0.010`;
- `key(startId, endId)` для стабильности сегментов между кадрами.

## Симптом 2: после завершения остаётся только заливка

### Наблюдение

После `confirmContour()` пользователь видел только fill.

### Корневые причины

1. **State (намеренный legacy UX):** `showContourPoints = !isContourConfirmed` скрывал зелёные точки сразу после confirm — это не баг рендера, а устаревший контракт.
2. **State:** `showSectionFill` требовал `isContourConfirmed`, поэтому до confirm заливки не было (расхождение с iOS/shared).
3. **Renderer:** линии при confirm оставались в state (`showContourLines == true`), но могли перекрываться полупрозрачным `ShapeNode` fill на почти той же высоте (`FILL_VISUAL_OFFSET_M = 0.001` vs старый line offset `0.003`).

### Исправление

- `showContourPoints` и `showSectionFill` приведены к shared/iOS контракту (см. таблицу выше).
- Линии поднимаются выше fill через `LINE_ABOVE_FILL_OFFSET_M` при `showSectionFill == true`.
- Точки остаются видимыми после confirm до включения плитки.

Линии по-прежнему скрываются только в tile mode (`isTileVisible == true`) — это ожидаемое поведение.

## Debug и регрессия

В debug-панели `FloorArScreen` добавлены поля:
- `Show pts`
- `Show lines`
- `Show fill`

### Unit-тесты

`FloorUiStateVisibilityTest` — контракт visibility для placement / closed / confirmed / tile.

Shared KMP (iOS + будущая миграция Android): `FloorArControllerTileTest`, `FloorContourUiStateTileVisibilityTest` в `:shared:ar:domain` — toggle/rotate/change tile, visibility flags. См. [ios-tile-placement.md](../archive/completed-plans/ios-tile-placement.md).

### Device QA

1. Поставить 2 точки → видны точки, синие линии, distance labels.
2. Замкнуть контур (≥3 точек) → fill появляется до confirm.
3. Нажать OK (confirm) → точки, линии и fill остаются при `Tile: Off`.
4. Включить плитку → точки и линии скрываются, fill/texture остаются.

## Ключевые файлы

| Файл | Изменение |
|------|-----------|
| `FloorModels.kt` | visibility flags |
| `FloorArRenderConfig.kt` | толщина/offset линий |
| `ArSceneLayer.kt` | динамический Y-offset линий |
| `FloorArScreen.kt` | debug flags |
| `FloorUiStateVisibilityTest.kt` | unit-тесты контракта |

## Дальнейшие шаги (не в этом плане)

- Постепенная миграция Android `FloorUiState` / `FloorArViewModel` на shared `FloorArController`, чтобы не поддерживать две модели.
- iOS tile placement parity — [ios-tile-placement.md](../archive/completed-plans/ios-tile-placement.md) (реализовано в коде, device QA на iPhone).
