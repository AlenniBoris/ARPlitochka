# Анализ Паритета Заливки Плиткой: iOS vs Android

> **Статус: РЕШЕНО (Июнь 2026)**
> Все расхождения в масштабе и яркости устранены через перенос геометрии в shared и использование Unlit материалов.

Документ-расследование по конкретному расхождению скриншотов iOS/Android. Канонический контракт заполнения плиткой находится в [`AR/TILE_FILLING.md`](../ar/PLATFORM_TILE_FILLING.md).

## Итоговое Решение

1. **Shared Geometry**: Логика выравнивания по самому длинному ребру вынесена в `shared/ar/domain/.../geometry/AlignedSectionGeometry.kt`. Обе платформы используют её для расчета bounds и локальных координат.
2. **Normalized UVs (Android)**: Исправлена проблема "растягивания" на больших зонах. Теперь `polygonPath` всегда 0..1, а реальный размер задается через `scale` узла.
3. **Unlit Materials**: Плитка больше не темнеет от теней AR-сцены. Используется `createImageInstance` на Android и `SCNLightingModelConstant` на iOS.
4. **Parity**: Масштаб, ориентация и яркость плитки теперь идентичны на обеих платформах.

---

Первый скриншот — iOS. Второй скриншот — Android. На обеих платформах пользователь поставил 4 точки в максимально похожих местах и получил визуально одну и ту же зону. После включения плитки паттерн отличается: на iOS плитка заметно крупнее и рисунок не совпадает с Android; на Android паттерн более плотный и ориентирован иначе.

## Короткий Вывод

Эталоном сейчас следует считать Android, потому что там алгоритм заполнения уже был спроектирован как bounds-aware pattern fill: полигон переводится в локальную систему координат по самому длинному ребру, затем по его локальным bounds генерируется bitmap-паттерн с физическим размером повтора `0.78 m x 1.04 m`.

iOS после последнего исправления уже отображает текстуру, но всё ещё не повторяет Android. Основная причина: iOS считает bounds и UV в world X/Z, а Android считает bounds в локальной, повернутой системе полигона. Из-за этого у iOS размер и ориентация pattern bitmap зависят от поворота зоны относительно мировых осей ARKit, а у Android — от геометрии самой зоны.

## Визуальное Сравнение Скриншотов

### iOS

На iOS видны крупные красно-серые полосы. В пределах зоны помещается меньше повторов паттерна. По восприятию плитка выглядит ближе к натуральному физическому размеру, но рисунок не совпадает с Android: полосы и масштаб отличаются даже при похожей зоне.

Дополнительный важный сигнал из debug overlay:

- `Tile: On`
- `Texture rotation: 0`
- `Tile type: paving_stones_v2`
- `Phase: contour`

Это означает, что shared state и iOS UI сработали корректно. Проблема не в событиях `ToggleTileVisibility` / `ChangeTileType`, а именно в texture mapping / pattern generation.

### Android

На Android зона заполнена более плотным паттерном. Полосы красной и серой плитки выглядят иначе по масштабу и направлению. Debug overlay подтверждает тот же режим:

- `Tile: On`
- `Texture rotation: 0`
- `Confirmed: true`
- `Show fill: Yes`

Поскольку Android уже содержит исходную референс-логику заполнения, именно его поведение нужно использовать как ground truth до отдельного продуктового решения о смене физического размера плитки.

## Как Заполняет Android

Ключевые файлы:

- `features/floor-detection/src/main/java/com/example/arplitka/features/floordetection/presentation/components/ArSceneLayer.kt`
- `features/floor-detection/src/main/java/com/example/arplitka/features/floordetection/presentation/utils/FloorArGeometryUtils.kt`
- `features/floor-detection/src/main/java/com/example/arplitka/features/floordetection/presentation/utils/FloorArTextureUtils.kt`
- `features/floor-detection/src/main/java/com/example/arplitka/features/floordetection/presentation/utils/FloorArRenderConfig.kt`

Алгоритм:

1. Android берёт точки полигона и считает centroid.
2. `toAlignedSectionGeometry()` выбирает самое длинное ребро контура.
3. Все точки переводятся в локальную 2D-систему: ось X идёт вдоль самого длинного ребра, ось Y — перпендикулярно ему.
4. `polygonPath.bounds()` считается уже в этой локальной системе, а не в world X/Z.
5. `toSectionPatternBitmap(widthMeters, heightMeters, rotationDegrees)` создаёт bitmap под размер зоны.
6. Размер повтора берётся из констант:
   - `TILE_TEXTURE_WIDTH_M = 0.78f`
   - `TILE_TEXTURE_HEIGHT_M = 1.04f`
7. `BitmapShader` с `Shader.TileMode.REPEAT` повторяет исходную текстуру внутри output bitmap.
8. Output bitmap кладётся в Filament material и применяется к `ShapeNode` с `uvScale = Float2(1f, 1f)`.
9. Сам `ShapeNode` поворачивается на `sectionGeometry.rotationY`, чтобы локальная геометрия зоны легла обратно в world.

Важное свойство Android: текстура живёт в локальной системе зоны. Паттерн не должен менять масштаб из-за того, что пользователь повернул телефон или контур относительно мировых AR-осей.

## Как Заполняет iOS

Ключевые файлы:

- `iosApp/src/iosMain/kotlin/com/example/arplitka/iosapp/platform/render/IosArContourRenderer.kt`
- `iosApp/src/nativeInterop/cinterop/plane_geometry_bridge.def`
- `iosApp/src/nativeInterop/headers/PlaneGeometryBridge.h`

Текущий алгоритм:

1. `IosArContourRenderer.syncSectionFill()` передаёт world X/Z точки в `pg_create_contour_fill_geometry`.
2. `pg_create_contour_fill_geometry()` строит fan geometry в world X/Z.
3. В bridge добавлены UV 0..1 по axis-aligned bounds этих world X/Z точек.
4. `sectionBoundsMeters()` на Kotlin-стороне тоже считает `minX/maxX/minZ/maxZ` в world X/Z.
5. `pg_create_tile_section_pattern_image()` создаёт bitmap по этим world-axis bounds.
6. Получившийся image применяется как `diffuse.contents` к SceneKit material.

Это уже лучше, чем белая заливка без UV, но всё ещё не Android parity. iOS сейчас фактически говорит: "растяни паттерн по axis-aligned bounding box в мировых координатах". Android говорит: "растяни паттерн по локальному bounding box зоны, выровненному по самому длинному ребру".

## Ключевые Причины Расхождения

### 1. Разная система координат bounds

Android:

- bounds считаются после проекции точек на локальные оси зоны;
- локальная X-ось совпадает с самым длинным ребром;
- размер bitmap соответствует длине/ширине зоны в этой системе.

iOS:

- bounds считаются по world X/Z;
- если зона повернута относительно world axes, axis-aligned bounds становятся больше или меньше локальных bounds;
- итоговый pattern bitmap получает другой physical-to-pixel масштаб.

Это главный источник различий.

### 2. Разная ориентация паттерна

Android поворачивает весь `ShapeNode` через `sectionGeometry.rotationY`, то есть паттерн наследует orientation зоны.

iOS геометрия уже лежит в world X/Z, а UV вычислены по world axes. Поэтому `TextureRotation.DEGREES_0` на iOS означает "0 градусов относительно world X/Z", а на Android — "0 градусов относительно локальной системы зоны".

На скриншотах это проявляется как несовпадение направления красно-серых полос.

### 3. Разная генерация tiled bitmap

Android использует `BitmapShader(REPEAT)`, а iOS вручную рисует исходное изображение в цикле `drawInRect`.

Сама идея может быть эквивалентной, но для parity нужно, чтобы обе реализации получали одинаковые входы:

- одинаковую локальную ширину зоны;
- одинаковую локальную высоту зоны;
- одинаковый anchor/offset паттерна;
- одинаковый rotation относительно локальной зоны.

Сейчас входы разные.

### 4. Возможный image scale нюанс на iOS

iOS использует `UIImage.size`, которая измеряется в points, а не всегда в raw pixels. Android использует `Bitmap.width/height` в pixels.

Если PNG попадает в bundle как обычный file resource без `@2x/@3x`, `UIImage.size` обычно совпадает с pixel dimensions. Но для инженерной надёжности iOS bridge лучше считать `CGImageGetWidth(source.CGImage)` / `CGImageGetHeight(source.CGImage)`, чтобы не зависеть от scale.

Это не выглядит главной причиной текущего расхождения, но это нужно зафиксировать как обязательную правку для точного parity.

## Какая Заливка Правильная

Правильная заливка для текущего проекта — Android.

Причины:

- Android был продуктовым референсом для полного flow "контур → OK → плитка".
- Android уже реализует alignment по геометрии зоны, а не по world axes.
- Android использует физические константы `0.78 m x 1.04 m` при генерации section bitmap.
- В документации проекта Android отмечен как эталон для полного AR tile flow.

Важно: если продуктово окажется, что визуальный масштаб Android слишком мелкий, тогда нужно менять ground truth на уровне общих требований и синхронно менять обе платформы. Но при задаче "добиться parity" исправлять нужно iOS под Android.

## План Паритета

### Шаг 1. Вынести общую геометрию alignment в shared

Нужно не дублировать Android-логику руками на iOS, а сделать общий KMP helper, например:

```kotlin
data class AlignedSectionGeometry(
    val localPoints: List<ArPoint2D>,
    val centroidX: Float,
    val centroidZ: Float,
    val axisX: Float,
    val axisZ: Float,
    val perpendicularX: Float,
    val perpendicularZ: Float,
    val rotationYDegrees: Float,
    val boundsWidthM: Float,
    val boundsHeightM: Float
)
```

Общий алгоритм должен повторять Android:

1. Найти самое длинное ребро закрытого полигона.
2. Нормализовать его как локальную X-ось.
3. Построить перпендикуляр.
4. Спроецировать все world X/Z точки относительно centroid в local X/Y.
5. Посчитать bounds по local points.
6. Вернуть rotationY для рендера.

Android после этого тоже может использовать shared helper, чтобы исключить расхождение.

### Шаг 2. Переделать iOS fill geometry на local polygon + transform

Сейчас `pg_create_contour_fill_geometry` создаёт вершины сразу в world X/Z. Для parity лучше сделать как Android:

1. Kotlin считает aligned geometry.
2. В bridge передаётся `localPoints`.
3. `pg_create_contour_fill_geometry` строит плоский polygon в local XY/SCN XZ.
4. SceneKit node получает:
   - `position = centroid`
   - `eulerAngles.y = rotationYDegrees`

Так UV 0..1 будут соответствовать локальному bounds, а не world bounds.

Минимальная альтернатива: оставить world-geometry, но вычислять UV через inverse alignment transform. Это сложнее поддерживать и хуже совпадает с Android mental model.

### Шаг 3. Генерировать iOS pattern bitmap по local bounds

`pg_create_tile_section_pattern_image()` должна получать:

- `widthMeters = aligned.boundsWidthM`
- `heightMeters = aligned.boundsHeightM`
- `rotationDegrees = textureRotation.degrees`

А не world-axis `maxX-minX` / `maxZ-minZ`.

### Шаг 4. Синхронизировать pixel math

iOS bridge лучше перевести с `UIImage.size` на raw CGImage dimensions:

```objc
CGFloat srcW = (CGFloat)CGImageGetWidth(source.CGImage);
CGFloat srcH = (CGFloat)CGImageGetHeight(source.CGImage);
```

Формулы должны остаться такими же, как Android:

```text
outputWidthPx  = sourceWidthPx  / TILE_TEXTURE_WIDTH_M  * sectionWidthMeters
outputHeightPx = sourceHeightPx / TILE_TEXTURE_HEIGHT_M * sectionHeightMeters
```

Ограничения тоже должны совпадать:

- min 64 px
- max 2048 px

### Шаг 5. Зафиксировать offset паттерна

Даже при одинаковом масштабе паттерн может быть сдвинут. Сейчас Android начинает shader от `(0, 0)` output bitmap после rotation вокруг центра. Для iOS parity нужно:

- начинать tiled draw с тех же координат, что Android drawRect;
- вращать canvas вокруг центра output bitmap;
- не добавлять platform-specific offset от minX/minZ.

Если нужен стабильный "мировой" offset, это отдельное продуктовое решение. Для parity с Android нужно повторить Android.

### Шаг 6. Добавить regression tests для чистой математики

Добавить common tests для aligned geometry:

- прямоугольник 2.0 x 1.0 m без поворота даёт bounds 2.0 x 1.0;
- тот же прямоугольник, повернутый на 30°, всё равно даёт bounds 2.0 x 1.0;
- rotationY совпадает с направлением самого длинного ребра;
- для одинаковых точек Android/iOS получают одинаковые `boundsWidthM`, `boundsHeightM`, `rotationYDegrees`.

## Конкретный Вероятный Fix

Самая вероятная правка:

1. Создать shared `buildAlignedSectionGeometry(points: List<ArPoint3D>)`.
2. Android `FloorArGeometryUtils.toAlignedSectionGeometry()` заменить или сверить с shared helper.
3. iOS `IosArContourRenderer.sectionBoundsMeters()` удалить как world-axis calculation.
4. В `IosArContourRenderer.syncSectionFill()` использовать aligned local points:
   - для geometry;
   - для material cache key;
   - для `pg_create_tile_section_pattern_image`.
5. В `pg_create_contour_fill_geometry()` оставить UV 0..1, но подавать туда local polygon points.
6. Node позиционировать в centroid и поворачивать как Android.

После этого iOS должен визуально совпасть с Android по:

- количеству повторов плитки внутри зоны;
- направлению полос при `TextureRotation.DEGREES_0`;
- поворотам 45/90/135;
- смене `paving_stones_v1/v2`.

## Проверка После Исправления

Manual QA:

1. Поставить два телефона рядом.
2. Разметить максимально одинаковый четырёхугольник.
3. Включить `paving_stones_v2`, rotation 0°.
4. Сравнить количество красных/серых полос вдоль длинной стороны.
5. Повернуть 45°, 90°, 135° — направление должно меняться одинаково.
6. Сменить плитку на v1 — масштаб должен оставаться одинаковым.
7. Проверить повернутую относительно комнаты зону: parity должен сохраняться независимо от world orientation.

Build checks:

```bash
./gradlew.bat :shared:ar:domain:testDebugUnitTest
./gradlew.bat :app:compileDebugKotlin
./gradlew :iosApp:linkDebugFrameworkIosArm64
```

## Итог

Проблема не в shared state и не в выборе `TileType`. На обоих скриншотах tile mode включён корректно.

Проблема в renderer math: Android мапит плитку в локальной системе зоны, iOS — в world-axis bounds. Для паритета нужно перенести Android alignment model в shared и заставить iOS строить fill/UV/pattern bitmap от той же локальной геометрии.
