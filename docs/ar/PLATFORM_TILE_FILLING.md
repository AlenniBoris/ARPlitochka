# Заливка плиткой в AR

Этот документ — канонический контракт заполнения финализированного AR-контура текстурой плитки.

## Продуктовый контракт

После финализации замкнутого контура:

1. Появляются элементы управления плиткой (tile controls).
2. Пользователь может добавить или убрать заливку плиткой.
3. Пользователь может переключаться между `paving_stones_v1` и `paving_stones_v2`.
4. Пользователь может поворачивать текстуру по циклу `0° -> 45° -> 90° -> 135° -> 0°`.
5. Пока включён режим плитки (tile mode), точки и линии контура скрыты.
6. Заливка зоны остаётся видимой и использует текстуру вместо синего материала заливки.

## Общее состояние (Shared State)

Референсные файлы:

- `shared/ar/domain/.../TileModels.kt`
- `shared/ar/domain/.../FloorContourUiState.kt`
- `shared/ar/contracts/.../FloorArContracts.kt`
- `shared/ar/domain/.../FloorArController.kt`

Состояние (State):

- `isTileVisible`
- `selectedTileType`
- `textureRotation`

События (Events):

- `ToggleTileVisibility`
- `ChangeTileType`
- `RotateTexture`

Правила:

- `ToggleTileVisibility` разрешён только когда контур финализирован и замкнут.
- `ChangeTileType` разрешён только в режиме плитки (tile mode).
- `RotateTexture` разрешён только в режиме плитки.
- `FinalizeArea` устанавливает `isFinalized = true` и `isTileVisible = false`.
- `Reset` сбрасывает состояние плитки к значениям по умолчанию.

## Референсный алгоритм (Shared)

Референсные файлы:

- `shared/ar/domain/.../geometry/AlignedSectionGeometry.kt`
- `features/floor-detection/.../ArSceneLayer.kt`
- `iosApp/.../IosArContourRenderer.kt`

Алгоритм:

1. Найти самое длинное ребро закрытого полигона.
2. Построить локальную систему координат зоны (Aligned Geometry).
3. Спроецировать точки в локальные XY (0..bounds).
4. Сгенерировать растровое изображение секции (section bitmap) под размер локальных границ.
5. Использовать **Unlit** материал (без влияния теней/света AR-сцены) для максимальной яркости.
6. На Android: `ShapeNode` с нормализованным `polygonPath` (0..1) и масштабом (`scale`) узла.
7. На iOS: SceneKit geometry с UV 0..1.

Это гарантирует полный паритет масштаба и яркости между платформами.

## Повторяющийся узор (Repeat Pattern) и размер плитки

`repeatPattern` — это физический размер одного полного повторяющегося изображения-текстуры на полу.

Текущие мок-данные:

- `repeatPattern.widthMm = 780`
- `repeatPattern.lengthMm = 1040`

Текущие константы рендерера:

- Android: `TILE_TEXTURE_WIDTH_M = 0.78f`, `TILE_TEXTURE_HEIGHT_M = 1.04f`
- iOS bridge: `PG_TILE_TEXTURE_WIDTH_M = 0.78f`, `PG_TILE_TEXTURE_HEIGHT_M = 1.04f`

Материал плитки теперь всегда **Unlit** (`createImageInstance` на Android, `SCNLightingModelConstant` на iOS), чтобы цвета соответствовали исходному PNG независимо от освещения комнаты.

## Рендерер iOS

iOS полностью синхронизирован с Android через общий `AlignedSectionGeometry`. Больше нет расхождения в масштабе из-за границ по мировым осям.

## Эталон (Ground Truth)

Обе платформы теперь используют единую математику выравнивания и рендеринга. Android и iOS являются равноправными референсами для визуального контроля.

## Тесты

Общие доменные тесты (Shared/domain tests) должны покрывать:

- Переключение видимости плитки.
- Смену типа плитки.
- Цикл поворота текстуры.
- Флаги видимости в режиме плитки.
- Математику выровненной геометрии (aligned geometry) после переноса в shared.

Ручное тестирование (Manual QA):

1. Поставить два телефона рядом.
2. Разметить одинаковую четырёхточечную зону.
3. Включить `paving_stones_v2`, поворот `0°`.
4. Сравнить количество и направление повторяющихся полос.
5. Проверить повороты 45/90/135 градусов.
6. Переключить v1/v2.
7. Выполнить повторное сканирование (rescan) и проверить сброс состояния плитки.

## Связанные исторические документы

- `docs/archive/completed-plans/ios-tile-placement.md`
- `docs/investigations/tile-fill-parity-analysis.md`
- `docs/BACKEND_MOCKING_PLAN.md`
