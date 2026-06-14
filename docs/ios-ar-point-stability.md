# iOS: стабильность точек контура и коррекция после релокализации

**Статус:** реализовано в коде (июнь 2026), проверено на устройстве  
**Ветка:** `feature/ios_ar_screen`

Исходная постановка задачи: [ios-ar-point-stability-task.md](./ios-ar-point-stability-task.md).

Связанные документы:
- [IOS_AR_CONTINUOUS_FLOOR_PLACEMENT_PLAN.md](./IOS_AR_CONTINUOUS_FLOOR_PLACEMENT_PLAN.md) — placement patch, `workingFloorY`, render loop
- [SURFACE_DETECTION.md § iOS Debug Panel](./SURFACE_DETECTION.md#ios-debug-panel--справочник-полей) — поле `Anchor corr`
- [ios-tile-placement.md](./ios-tile-placement.md) — плитка после finalize (toggle, rotate, смена типа)
- [ios-ar-delegate-starvation-handoff-prompt.md](./ios-ar-delegate-starvation-handoff-prompt.md) — starvation delegate (отдельная проблема)

---

## 1. Проблема

После кратковременного фриза трекинга или релокализации ARKit сдвигает world space. Если точки контура хранятся как «сырые» мировые координаты, вся цепочка визуально «плывёт» относительно физического пола.

**Цель UX:** точки остаются там, где пользователь их поставил, пока смещение не станет заметным; крупные сдвиги — только по явному действию пользователя.

---

## 2. Архитектура (реализовано)

### 2.1. Хранение точек

| Элемент | Реализация |
|---------|------------|
| Root anchor | Один `contourRootAnchor` создаётся при **первой** точке (`pg_session_add_anchor_from_column_major`) |
| Координаты точек | `rootLocalX` / `rootLocalZ` относительно root anchor (не сырые world coords) |
| Fallback | `tapWorldPosition` + `lastResolvedWorldPosition` при потере anchor |
| Плоскость пола | `workingFloorY` / `lastAcceptedFloorY`; проекция через `FloorGeometry.projectToSectionFloor` |

**Почему один root, а не anchor на каждую точку:** контур двигается как единое тело; форма и расстояния между точками сохраняются.

### 2.2. Ключевые файлы

| Файл | Роль |
|------|------|
| `IosFloorAnchorStore.kt` | Root anchor, local XZ, политика коррекций, manual realign |
| `IosArSessionCoordinator.kt` | Tracking gates, sync anchor points, UI callbacks |
| `IosArPlacementRules.kt` | Пороги коррекции, debug-форматирование |
| `IosArContourRenderer.kt` | Отрисовка точек/линий/заливки/distance labels по `placedPoints` из domain |
| `IosArScreen.ios.kt` | Кнопки «+»/undo/OK, «Удалить зону», «Выровнять контур», подсказка |

### 2.3. Что не смешивать с коррекцией точек

| Подсистема | Поведение |
|------------|-----------|
| Placement explore patch | Двигается в `renderer(updateAtTime:)` через ray ∩ `workingFloorY`, **без** `session.currentFrame` в render loop |
| World-aligned grid | `pg_create_world_aligned_placement_patch_grid_geometry` — линии в world space |
| Anchor sync точек | Только в `handlePlacementDidUpdateFrame`, не в render loop |
| Частота sync в UI | Каждые `PLACEMENT_ANCHORED_POINTS_SYNC_INTERVAL_FRAMES` (6) для `floorArController.onFrame`; оценка коррекции — **каждый** placement-кадр |

---

## 3. Политика коррекций (три уровня)

Измеряются **две** дельты (см. debug `Anchor corr`):

- **`r:`** — сдвиг origin root-anchor vs последний принятый origin
- **`d:`** — max горизонтальное расхождение между candidate-позицией и тем, что сейчас показано (`lastResolvedWorldPosition`)

Для решения берётся `correctionDelta = max(r, d)`.

| Уровень | Порог | Поведение |
|---------|-------|-----------|
| **Micro** | ≤ 3 см | Применить сразу при стабильном трекинге (`micro`) |
| **Small** | 3–8 см | После нестабильности трекинга: авто через **2** стабильных кадра (`auto-small`) |
| **Macro** | ≥ 8 см | **Не** применять автоматически; кнопка «Выровнять контур» (`offer-realign`) |

### 3.1. Когда показывается кнопка «Выровнять контур»

- Оранжевая кнопка над кнопками «+» / undo
- Подсказка вверху: *«Контур мог сместиться — нажмите «Выровнять», если точки не на полу»*
- Подсказка **не блокирует** постановку новых точек (отдельна от `placementHint`)
- Нажатие применяет **актуальный** transform anchor из текущего `ARFrame`

### 3.2. Флаг нестабильности

`placementAnchorHadInstability` в coordinator:

- `true` после `limited` / `relocalizing` / interruption / `Camera gap ≥ 500 ms`
- Сбрасывается после успешного «Выровнять», очистки контура или когда рассогласование исчезло
- Не сбрасывается, пока `Anchor corr` в `frozen-unstable` / `pending-small` (иначе auto-small после фриза не успевает примениться)

### 3.3. Защита от ложных срабатываний

- Оценка коррекции только по anchor из **текущего** `ARFrame` (без stale fallback)
- Transient-скачки: signature candidate должен быть стабилен несколько кадров
- Во время нестабильного трекинга — заморозка отображения (`frozen-unstable`)

---

## 4. Константы (`IosArPlacementRules.kt`)

| Константа | Значение | Назначение |
|-----------|----------|------------|
| `PLACEMENT_ANCHOR_MICRO_CORRECTION_M` | 0.03 m | Мгновенное применение |
| `PLACEMENT_ANCHOR_MACRO_BLOCKED_M` | 0.08 m | Порог кнопки / manual realign |
| `PLACEMENT_ANCHOR_SMALL_AUTO_CONFIRM_FRAMES` | 2 | Кадры для auto-small |
| `PLACEMENT_ANCHOR_CONFIRM_FRAMES` | 3 | Подтверждение macro-candidate |
| `PLACEMENT_ANCHOR_SIGNATURE_TOLERANCE_M` | 0.015 m | Квантование signature |
| `PLACEMENT_ANCHORED_POINTS_SYNC_INTERVAL_FRAMES` | 6 | Периодический sync точек в domain/UI |
| immediate contour sync | при сдвиге ≥ 1 см | Немедленный push в domain после anchor-correction |
| `PLACEMENT_ANCHOR_RECOVERY_CONTEXT_SECONDS` | 30 s | Legacy recovery window |
| `TRACKING_DEGRADED_CAMERA_GAP_MS` | 500 ms | Порог деградации трекинга |

---

## 5. Debug: поле `Anchor corr`

Формат: `{state} r:{rootCm} d:{displayCm}`

| state | Значение |
|-------|----------|
| `micro` | Микро-коррекция применена |
| `auto-small` | Авто-применение 3–8 см после фриза |
| `pending-small` | Ждём стабильности для auto-small |
| `offer-realign` | Доступна кнопка выравнивания |
| `pending-macro` | Крупный candidate, кнопка ещё не latched |
| `frozen-unstable` | Трекинг нестабилен, показ заморожен |
| `manual` | Пользователь нажал «Выровнять» |
| `no-frame-anchor` | Root anchor не найден в текущем кадре |

---

## 6. Результаты тестов на устройстве (июнь 2026)

| Сценарий | Ожидание | Результат |
|----------|----------|-----------|
| Лёгкий фриз, сдвиг ~несколько см | Auto-small без кнопки | ⏳ проверить после последнего патча |
| Закрыть камеру, сдвиг ~30 см | Кнопка + manual realign | ✅ `frozen-unstable r:30.3 d:30.3`, кнопка, после нажатия — ок |
| Обычная работа без фриза | Нет рывков, `micro` | ✅ |

---

## 7. Контур после close / finalize

| Состояние | Точки | Линии | Labels | Заливка | `Closed` |
|-----------|-------|-------|--------|---------|----------|
| `Closed: Yes`, placement | видны | видны | видны | видна (preview) | proximity snap |
| После галочки (`FinalizeArea`) | видны | видны | видны | видна | latched, не сбрасывается |
| После фриза / finalize | anchor-correction активна | замыкающее ребро сохраняется при `Closed: Yes` | обновляются на midpoint сегментов | сохраняется при `Closed: Yes` | не сбрасывается `FloorSnapReducer` |

После `FinalizeArea` coordinator продолжает вызывать `anchorStore.placedPoints(..., trackingStable, hadTrackingInstability)` в scan-пути, а не только в placement-пути.

### 7.1. Сброс зоны и пересканирование

- Отдельной кнопки «Удалить зону» нет: **«Пересканировать»** в нижней колонке очищает контур (`FloorArEvent.Reset`) и перезапускает поиск рабочей плоскости
- Кнопка видна всегда (до первой точки, во время расстановки и после finalize)
- После reset: `Points: 0`, `Closed: No`, `Finalized: No`; белая сетка снова ищется через scan reset

### 7.2. Нижняя колонка кнопок

Сверху вниз, по условиям:
1. **«Выровнять контур»** — оранжевая, когда доступна anchor-correction
2. **Undo / «+» / OK** — только в placement (`showContourActions`)
3. **«Пересканировать»** — всегда

### 7.3. Distance labels (плашки расстояния)

- Рендерятся в `IosArContourRenderer.syncDistanceLabels()` через `SCNPlane` + `pg_create_contour_distance_label_image`
- Геометрия сегментов/формат расстояния — `shared/ar/domain/geometry/ContourSegmentLabels.kt` (planar XZ)
- Ориентация вдоль сегмента: `contourLineRotationYDegrees` + `eulerAngles` в радианах (SceneKit)
- Labels привязаны к `showContourLines`; cache по batch key; позиция обновляется при anchor-correction

---

## 8. Известные ограничения

1. **Свободный `ARAnchor`** не привязан к `ARPlaneAnchor` — при некоторых типах drift коррекция ARKit может не совпадать с визуальным полом. Следующий шаг (не реализован): привязка root к plane anchor первого тапа.
2. **Delegate starvation** (~10 с между `didUpdateFrame`) — отдельная проблема; placement patch частично обходится через render loop, sync точек — нет.
3. **Мелкий дрейф < 3 см** без фриза — применяется тихо; пользователь не замечает.
4. **Крупный сдвиг без нажатия кнопки** — намеренно: визуальная стабильность важнее слепого доверия ARKit.

---

## 9. Чеклист регрессии

1. Поставить 3–5 точек, походить — контур стабилен, `Anchor corr: micro`
2. Краткий фриз (без закрытия камеры) — при сдвиге 3–8 см точки возвращаются сами (`auto-small`)
3. Закрыть камеру на 2–3 с — точки не прыгают сами; при сдвиге ≥ 8 см — кнопка «Выровнять»
4. Нажать «Выровнять» — контур переезжает, `Anchor corr: manual`, кнопка исчезает
5. После первой точки — нет фриза delegate (anchor lookup не в render loop)
6. Undo всех точек — root anchor удалён, кнопка скрыта
7. Замкнуть контур до галочки — зелёные точки, синие линии, distance labels и preview-заливка видны (`Closed: Yes`, `Finalized: No`)
8. Нажать галочку — точки, линии, labels, заливка и замыкающее ребро остаются (`Closed: Yes`, `Finalized: Yes`)
9. Фриз после finalize — контур не «открывается», anchor-correction и realign работают как в placement
10. Поставить 2+ точки — distance labels на midpoint каждого сегмента; при замыкании — label на closing edge
11. Нажать «Пересканировать» с точками на полу — контур очищается, сессия пересканирует плоскость, `Points: 0`
12. После finalize нажать «Пересканировать» — зона сбрасывается и начинается новый поиск пола

---

## 10. История

| Дата | Изменение |
|------|-----------|
| Июнь 2026 | Исходная задача: anchor вместо raw world coords |
| Июнь 2026 | Root anchor + local XZ + transient debounce |
| Июнь 2026 | Трёхуровневая политика, display delta, кнопка «Выровнять» |
| Июнь 2026 | Auto-small (3–8 см) после нестабильности трекинга |
| Июнь 2026 | Preview-fill при `Closed: Yes`, latched closed после finalize, anchor sync post-finalize |
| Июнь 2026 | Точки остаются после finalize; render-loop sync контура при delegate starvation |
| Июнь 2026 | Насыщенная синяя заливка и двойной winding (нормали вверх/вниз) |

---

*При изменении `IosFloorAnchorStore` или порогов в `IosArPlacementRules.kt` обновлять этот файл.*
