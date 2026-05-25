# Module `:shared:ar`

Shared-модуль для переиспользуемых ARCore utilities.

## Назначение

`shared:ar` содержит небольшие расширения и helper-функции, которые могут использоваться разными feature-модулями, работающими с ARCore.

Модуль не содержит UI, ViewModel, бизнес-состояние конкретной фичи или привязку к экрану. Его задача - дать общий технический слой для работы с ARCore primitives.

## Структура

```text
shared/ar/
├── build.gradle.kts
└── src/main/java/com/example/arplitka/shared/ar/
    └── ArExtensions.kt
```

## Основной Файл

### `ArExtensions.kt`

Содержит extension-функции для ARCore:

```kotlin
fun Plane.isUsableHorizontalPlane(): Boolean
fun Plane.area(): Float
fun Frame.centerPlaneHit(viewportSize: IntSize): HitResult?
```

## Что Делают Расширения

### `Plane.isUsableHorizontalPlane()`

Проверяет, что плоскость:

- направлена вверх;
- находится в `TrackingState.TRACKING`.

Используется для фильтрации пола и исключения неподходящих плоскостей.

### `Plane.area()`

Возвращает приблизительную площадь ARCore-плоскости:

```kotlin
extentX * extentZ
```

Это техническая оценка площади найденной плоскости, а не точный расчет пользовательского полигона.

### `Frame.centerPlaneHit(...)`

Делает hit-test из центра viewport:

- берет центр экрана;
- вызывает `Frame.hitTest`;
- выбирает первый hit по подходящей горизонтальной плоскости;
- дополнительно проверяет `plane.isPoseInPolygon(hit.hitPose)`.

Используется reticle-сценарием: пользователь наводит центр экрана на пол, а feature-модуль получает точку попадания.

## Использованные Технологии

- Kotlin;
- AndroidX `IntSize`;
- ARCore `Frame`, `Plane`, `HitResult`, `TrackingState`;
- SceneView dependency подключена на уровне модуля для совместимости с AR-фичами.

## Зависимости Модуля

Основные зависимости:

- Kotlin stdlib;
- AndroidX Core KTX;
- Compose UI;
- ARCore;
- SceneView AR.

## Границы Ответственности

В этот модуль можно добавлять:

- extension-функции над ARCore `Frame`, `Plane`, `Pose`, `HitResult`;
- общие геометрические helper-функции для AR;
- небольшие технические utilities без знания о конкретном экране.

В этот модуль не стоит добавлять:

- Compose UI;
- ViewModel;
- feature-specific state;
- тексты ресурсов конкретной фичи;
- сетевые запросы;
- расчет раскладки плитки.

## Когда Использовать

Использовать `:shared:ar`, если логика:

- нужна нескольким AR-фичам;
- не зависит от UI;
- не знает о `FloorUiState`;
- работает с ARCore primitives напрямую.
