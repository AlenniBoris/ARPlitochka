# Module `:features:floor-detection`

Feature-модуль основной AR-функциональности: поиск пола, разметка контура зоны и визуализация плитки.

## Назначение

Модуль отвечает за пользовательский сценарий выделения зоны на полу:

- настройка AR-сессии;
- поиск горизонтальной поверхности;
- hit-test из центра экрана;
- отображение ретикла и статусов;
- добавление точек контура;
- соединение точек линиями;
- отображение подписей длин;
- замыкание полигона при приближении к первой точке;
- финализация секции;
- отображение заливки и текстуры плитки;
- сброс выделенной секции.

Это главный фронтовый модуль текущего приложения.

## Структура

```text
features/floor-detection/
├── build.gradle.kts
└── src/main/
    ├── assets/
    │   └── textures/
    │       └── paving_stones.png
    ├── java/com/example/arplitka/features/floordetection/
    │   ├── data/
    │   │   └── repository/
    │   │       └── FloorDetectorRepositoryImpl.kt
    │   ├── di/
    │   │   └── FloorDetectionModule.kt
    │   ├── domain/
    │   │   ├── model/
    │   │   │   └── FloorModels.kt
    │   │   ├── repository/
    │   │   │   └── IFloorDetectorRepository.kt
    │   │   └── usecase/
    │   │       └── ProcessArFrameUseCase.kt
    │   └── presentation/
    │       ├── screen/
    │       │   └── FloorArScreen.kt
    │       └── viewmodel/
    │           └── FloorArViewModel.kt
    └── res/
```

## Архитектурные Слои

### `presentation`

UI и состояние экрана.

Содержит:

- `FloorArScreen`;
- `FloorArViewModel`.

`FloorArScreen` отвечает за Compose UI, `ARSceneView`, SceneView nodes, материалы, текстуру плитки и визуальные элементы.

`FloorArViewModel` управляет `FloorUiState` через `MutableStateFlow`, обрабатывает результаты AR-кадров, добавляет/удаляет точки, замыкает полигон и финализирует секцию.

### `domain`

Доменный слой фичи.

Содержит:

- модели `FloorUiState`, `ArPoint`, `ArFrameResult`;
- enum `FloorDetectionState`;
- интерфейс `IFloorDetectorRepository`;
- use case `ProcessArFrameUseCase`.

Доменный слой задает контракт обработки AR-кадра и не должен зависеть от конкретной реализации repository.

### `data`

Реализация источника AR-данных.

`FloorDetectorRepositoryImpl` получает `Session`, `Frame` и размер viewport, фильтрует горизонтальные плоскости, делает hit-test по центру экрана и возвращает `ArFrameResult`.

Repository не управляет UI-состоянием. UI-состояние собирается во ViewModel.

### `di`

Hilt-модуль `FloorDetectionModule`, который связывает `IFloorDetectorRepository` с `FloorDetectorRepositoryImpl` в `ViewModelComponent`.

## Использованные Технологии

- Kotlin;
- Jetpack Compose;
- Material 3;
- StateFlow;
- Lifecycle Compose;
- Hilt;
- ARCore;
- SceneView / ARSceneView;
- Filament texture/material API через SceneView;
- Android assets для хранения текстуры плитки.

## AR-Сценарий

1. `ARSceneView` создает AR-сессию.
2. Конфигурация включает горизонтальный поиск плоскостей:
   - `Config.PlaneFindingMode.HORIZONTAL`;
   - `Config.LightEstimationMode.ENVIRONMENTAL_HDR`;
   - `Config.DepthMode.AUTOMATIC`, если доступен;
   - `Config.FocusMode.AUTO`;
   - `Config.UpdateMode.LATEST_CAMERA_IMAGE`.
3. На каждом AR-кадре вызывается `viewModel.onSessionUpdated(...)`.
4. Use case передает кадр в repository.
5. Repository возвращает `ArFrameResult`.
6. ViewModel обновляет `FloorUiState`.
7. Compose перерисовывает AR-узлы и UI.

## Разметка Контура

Точки добавляются по текущему hit-test результату из центра экрана. Для стабилизации используются ARCore `Anchor`.

`ArPoint` хранит:

- `Anchor`;
- актуальный `Pose`;
- `id`.

В каждом AR-кадре ViewModel обновляет pose всех точек из anchor, чтобы Compose получал новое состояние и корректно перерисовывал контур после изменений ARCore tracking, включая возврат приложения из background.

## Замыкание Полигона

ViewModel проверяет расстояние от текущего reticle hit pose до первой точки:

- если точек минимум 3 и расстояние меньше `CLOSE_THRESHOLD_M`, полигон считается замкнутым;
- если расстояние меньше `SNAP_THRESHOLD_M`, подсвечивается точка snapping;
- при замкнутом полигоне кнопка добавления превращается в `OK`;
- нажатие `OK` переводит секцию в `isFinalized = true`.

## Визуализация

Модуль отображает:

- зеленые плоские точки через `CylinderNode`;
- синие линии через `CubeNode`;
- динамическую линию от последней точки до reticle;
- подписи длин через `ImageNode` с bitmap-лейблами;
- заливку полигона через `ShapeNode`;
- текстуру плитки после финализации.

Визуальный порядок:

1. заливка;
2. линии;
3. подписи;
4. точки.

Так точки остаются поверх линий и заливки.

## Текстура Плитки

Текстура хранится в:

```text
src/main/assets/textures/paving_stones.png
```

После финализации секции `ShapeNode` использует текстурированный материал. До финализации замкнутый полигон отображается обычной цветной заливкой.

UV-масштаб рассчитывается исходя из физического размера изображения плитки:

- ширина: `0.78 м`;
- высота: `1.04 м`.

Это нужно, чтобы текстура повторялась в реальном масштабе, а не растягивалась на весь полигон.

## Debug UI

Debug-панель отображается только при `BuildConfig.DEBUG`.

Показывает:

- количество найденных плоскостей;
- примерную площадь выбранной плоскости;
- состояние tracking;
- количество точек;
- замкнут ли полигон;
- финализирована ли секция.

## Зависимости Модуля

```kotlin
implementation(project(":shared:ar"))
implementation(project(":shared:ui"))
```

Также используются:

- `androidx.lifecycle:lifecycle-runtime-compose`;
- `androidx.hilt:hilt-navigation-compose`;
- `com.google.ar:core`;
- `io.github.sceneview:arsceneview`.

## Границы Ответственности

В модуль можно добавлять:

- новые AR-визуализации выделенной зоны;
- новые состояния разметки;
- сетевой use case отправки выделенного контура на backend;
- маппинг AR-точек в 2D backend DTO;
- UI выбора плитки/угла поворота, если это относится к текущему экрану.

Не стоит добавлять:

- расчет оптимальной раскладки плитки;
- работу с базой данных;
- хранение проектов на сервере;
- тяжелую оптимизацию обрезков;
- backend-specific бизнес-логику.

Эти задачи должны перейти в отдельный backend-проект, описанный в `../../BACKEND_ROADMAP.md`.

## Ближайшее Развитие

Следующий логичный шаг для этого модуля - подготовить экспорт выделенной зоны:

- преобразовать AR `x/z` координаты в 2D-полигон в метрах;
- нормализовать точки относительно первой точки или центра;
- добавить выбранный `tileCollectionId`, `textureId` и `rotationDegrees`;
- отправить данные в backend через отдельный repository/use case.
