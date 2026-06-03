# Модуль Floor Detection (Android)

Эталонный AR-экран: **поиск горизонтальных поверхностей** + **разметка контура** + **заливка и плитка**.

Полное описание поведения, договорённостей и чеклистов регрессии: **[docs/SURFACE_DETECTION.md](../../docs/SURFACE_DETECTION.md)** → разделы **Android — статус и договорённости** и **Android (ARCore) — реализация**.

## Структура

- `domain`: `FloorUiState`, `ArFrameResult`, `ProcessArFrameUseCase`, `IFloorDetectorRepository`
- `data`: `FloorDetectorRepositoryImpl` — только данные кадра, без UiState
- `presentation`:
  - `screen/FloorArScreen.kt` — Compose UI
  - `components/ArSceneLayer.kt` — ARCore + planeRenderer + 3D контур
  - `viewmodel/FloorArViewModel.kt` — состояние, точки, confirm, tile
  - `utils/` — геометрия, текстуры, константы рендера

## Кратко: что не ломать

- Нативный **`planeRenderer`** до `confirmContour`
- **Нет** фиксации одной плоскости на весь сеанс
- Center hit только с **`isPoseInPolygon`**
- Минимальная площадь **0.15 m²**
- Контур на **Y первой точки** (`sectionFloorY` в `ArSceneLayer`)

## Сборка

```bash
./gradlew :app:installDebug
```

Требуется устройство с ARCore.
