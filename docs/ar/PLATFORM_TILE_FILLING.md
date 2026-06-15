# Заливка Плиткой В AR

Этот документ — канонический контракт заполнения финализированного AR-контура текстурой плитки.

## Продуктовый Контракт

После финализации замкнутого контура:

1. Появляются tile controls.
2. Пользователь может добавить или убрать заливку плиткой.
3. Пользователь может переключаться между `paving_stones_v1` и `paving_stones_v2`.
4. Пользователь может поворачивать текстуру по циклу `0° -> 45° -> 90° -> 135° -> 0°`.
5. Пока включён tile mode, точки и линии контура скрыты.
6. Заливка зоны остаётся видимой и использует текстуру вместо синего fill material.

## Shared State

Референсные файлы:

- `shared/ar/domain/.../TileModels.kt`
- `shared/ar/domain/.../FloorContourUiState.kt`
- `shared/ar/contracts/.../FloorArContracts.kt`
- `shared/ar/domain/.../FloorArController.kt`

State:

- `isTileVisible`
- `selectedTileType`
- `textureRotation`

Events:

- `ToggleTileVisibility`
- `ChangeTileType`
- `RotateTexture`

Правила:

- `ToggleTileVisibility` разрешён только когда контур финализирован и замкнут.
- `ChangeTileType` разрешён только в tile mode.
- `RotateTexture` разрешён только в tile mode.
- `FinalizeArea` устанавливает `isFinalized = true` и `isTileVisible = false`.
- `Reset` сбрасывает tile state к значениям по умолчанию.

## Repeat Pattern И Размер Плитки

`repeatPattern` — это физический размер одного полного повторяющегося изображения-текстуры на полу. Это не обязательно размер одного отдельного камня или одного tile variant.

Текущие mock-данные:

- `repeatPattern.widthMm = 780`
- `repeatPattern.lengthMm = 1040`

Текущие renderer constants:

- Android: `TILE_TEXTURE_WIDTH_M = 0.78f`, `TILE_TEXTURE_HEIGHT_M = 1.04f`
- iOS bridge: `PG_TILE_TEXTURE_WIDTH_M = 0.78f`, `PG_TILE_TEXTURE_HEIGHT_M = 1.04f`

Размер изображения в пикселях используется для качества и плотности bitmap. Физический размер на AR-полу задаётся через `repeatPattern`, а не через pixel size картинки.

Архитектурный долг: Android и iOS renderers сейчас hardcode'ят `0.78 x 1.04 m`. Следующая стабильная схема должна передавать `repeatPattern` выбранной текстуры из tile catalog/shared model в AR renderer.

## Референсный Алгоритм Android

Референсные файлы:

- `features/floor-detection/.../ArSceneLayer.kt`
- `features/floor-detection/.../FloorArGeometryUtils.kt`
- `features/floor-detection/.../FloorArTextureUtils.kt`
- `features/floor-detection/.../FloorArRenderConfig.kt`

Алгоритм:

1. Посчитать centroid контура.
2. Найти самое длинное ребро контура.
3. Построить локальные координаты зоны, где local X идёт вдоль самого длинного ребра.
4. Посчитать local bounds по выровненному polygon.
5. Сгенерировать section bitmap через `toSectionPatternBitmap(widthMeters, heightMeters, rotationDegrees)`.
6. Повторить исходный bitmap через `BitmapShader(REPEAT)`.
7. Применить сгенерированный bitmap как fill material.
8. Повернуть отрисованную shape обратно в world space.

Это означает, что Android мапит текстуру в локальной системе координат контура, а не в world X/Z.

## iOS Renderer

Референсные файлы:

- `iosApp/.../IosArContourRenderer.kt`
- `iosApp/src/nativeInterop/cinterop/plane_geometry_bridge.def`
- `iosApp/src/nativeInterop/headers/PlaneGeometryBridge.h`

Текущее поведение iOS:

- SceneKit geometry строится через native bridge.
- Texture images загружаются из app bundle resources.
- Для tile material используется сгенерированный bitmap pattern.
- UV coordinates обязательны; без UV SceneKit может отрисовать белую или однотонную заливку.

Текущий parity gap:

- Android выравнивает bounds по самому длинному ребру контура.
- iOS сейчас считает fill bounds из world X/Z.
- Поэтому iOS pattern scale/orientation может отличаться от Android для той же физической зоны.

Ожидаемый fix: вынести aligned section geometry в shared code и заставить iOS использовать те же local bounds и local polygon geometry, что Android.

## Ground Truth

Пока продуктовые требования не говорят обратного, Android является референсом для tile filling, потому что он уже реализует полный AR tile flow и bounds-aware pattern generation в локальной системе зоны.

Если продукт позже решит, что масштаб Android неверный, обе платформы нужно менять вместе через `repeatPattern` / texture metadata, а не через platform-specific renderer tweaks.

## Тесты

Shared/domain tests должны покрывать:

- Toggle видимости плитки.
- Смену типа плитки.
- Цикл поворота текстуры.
- Visibility flags в tile mode.
- Математику aligned geometry после переноса в shared.

Manual QA:

1. Поставить два телефона рядом.
2. Разметить одинаковую четырёхточечную зону.
3. Включить `paving_stones_v2`, rotation `0°`.
4. Сравнить количество и направление повторяющихся полос.
5. Проверить повороты 45/90/135 градусов.
6. Переключить v1/v2.
7. Выполнить rescan и проверить сброс tile state.

## Связанные Исторические Документы

- `docs/archive/completed-plans/ios-tile-placement.md`
- `docs/investigations/tile-fill-parity-analysis.md`
- `docs/BACKEND_MOCKING_PLAN.md`
