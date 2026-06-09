# iOS: Continuous Floor Surface — план placement mode

**Статус:** фазы A–E + фиксы 0–4 реализованы; placement patch — render-driven (июнь 2026)  
**Дата:** июнь 2026  
**Контекст:** placement patch под прицелом отставал от delegate; решено через `renderer(updateAtTime:)` + world-aligned grid + One Euro Filter.

Связанные документы:
- [IOS_AR_SURFACE_STRATEGY.md](./IOS_AR_SURFACE_STRATEGY.md) — общая surface-стратегия iOS
- [SURFACE_DETECTION.md](./SURFACE_DETECTION.md) — константы, чеклисты Android/iOS
- [ios-ar-point-stability.md](./ios-ar-point-stability.md) — стабильность точек контура после релокализации

---

## 1. Проблема текущего подхода

После первой точки контура:

1. Большие scan-overlays **скрываются** (`enterPlacementHitOnlyMode`)
2. Показывается **маленькая сетка ~0.55 m** (`placementPatchNode`), позиция которой берётся из center hit каждый кадр
3. Экранный прицел (зелёная/красная зона) даёт мгновенную обратную связь, но **половая сетка «догоняет»** с задержкой

**Симптомы на устройстве:**
- Сетка на полу появляется через 2–3 секунды после переноса прицела
- Сетка и прицел не совпадают
- `Delegate Hz: 1` при нормальном `Camera gap` усугубляет задержку (см. Perf = `delegate blocked`)
- Точки при relocalization: anchor-коррекция с политикой micro / auto-small / manual macro — см. [ios-ar-point-stability.md](./ios-ar-point-stability.md)

**Корневая причина:** мы прячем уже найденную ARKit-плоскость и рисуем отдельный синтетический объект, который каждый кадр пересчитывается. Это архитектурный компромисс, а не только баг hit-test.

---

## 2. Предлагаемое решение

**Не чинить placement patch дальше.** Вернуться к **scan visualization** на всём этапе разметки контура (от 1-й точки до финализации), с жёсткой фильтрацией, чтобы не было дублей и наложений.

### UX-модель

```
Скан (0 точек):       ищем пол → показываем крупнейшую плоскость
Placement (1+ точек): та же логика scan-плоскости, зафиксированный уровень пола, одна сетка
                      прицел (зелёный/красный) = можно/нельзя ставить точку сейчас
                      точки контура = на ARAnchor-корне (не плывут)
Финал:                scan-сетка скрывается, остаётся контур
```

Пользователь видит **не «сетку, бегущую за прицелом»**, а **пол, который уже найден**. Прицел отвечает на вопрос «можно ли поставить точку здесь».

---

## 3. Что уже есть в коде (опереться при реализации)

| Механизм | Файл / место | Назначение |
|----------|--------------|------------|
| `MAX_SURFACE_OVERLAYS = 1` | `IosArPlaneSurfaceRenderer` | Максимум одна сетка |
| `pickVisibleAnchors` | `IosArPlaneSurfaceRenderer` | Выбор по площади, высоте, дистанции |
| `sectionFloorLocked` + `lockSectionFloor` | `IosArPlaneSurfaceRenderer` / `IosArScreen` | Фиксация уровня пола после 1-й точки |
| `isAllowedByLockedSectionFloor` | `IosArPlaneSurfaceRenderer` | Отсекает плоскости на другой высоте |
| `pinOverlayNodeToAnchorWorldFloor` | `IosArPlaneSurfaceRenderer` | Overlay в scene root, Y зафиксирован |
| `tryPinVisibleOverlays` / `enterPlacementScanFreeze` | `IosArPlaneSurfaceRenderer` | Pin overlays в world (есть, мало используется в screen-потоке) |
| `FocusedPlaneTracker` | `IosArPlaneRenderer` | Удержание фокуса на плоскости под прицелом |
| `IosFloorAnchorStore` + `ARAnchor` root | `IosFloorAnchorStore` | Стабильные точки контура |
| Цветной прицел `CenterReticle` | `ArReticle.kt` | Мгновенная валидация valid/invalid |

---

## 4. Алгоритм фильтрации (концепт)

### 4.1. Режим «Continuous Floor Surface»

После первой точки **не вызывать** `enterPlacementHitOnlyMode` (не скрывать scan-overlays). Продолжать `syncScanSurfaceVisualization` с ужесточёнными правилами.

### 4.2. Одна поверхность — один overlay

- Всегда `MAX_SURFACE_OVERLAYS = 1`
- `placementPatchNode` **не показывать** (или только как fallback при потере трекинга)
- Экранный прицел остаётся как вторичный индикатор

### 4.3. Выбор плоскости (приоритеты)

1. **`sectionPlaneAnchorId`** — якорь первого тапа, если ещё трекается и в полосе высоты
2. Иначе **`centerHit.anchor`** — плоскость прямо под прицелом
3. Иначе **крупнейшая** горизонтальная плоскость в полосе `sectionFloorY ± 0.10 m`

После выбора — **sticky**: не переключаться на другую плоскость, пока:
- не потерян трекинг, или
- якорь явно ушёл с уровня пола (> 0.10 m)

### 4.4. Фильтр дублей и наложений

| Правило | Порог / логика |
|---------|----------------|
| По высоте | `\|anchorY - sectionFloorY\| ≤ 0.10 m` — остальное скрыть |
| По площади на одном уровне | Две плоскости на одной высоте → только `max(area)` |
| По классификации | table/seat выше пола → suppress (уже есть) |
| По дистанции | Дальше 10 m от камеры → cull (уже есть) |
| При merge якорей ARKit | Не создавать новый overlay; обновить привязку к выжившему id или держать world-pinned геометрию до стабилизации |

### 4.5. Анти-дрейф (критично — причина прошлых багов)

После 1-й точки:

- Overlay **не child якоря**, а **pinned к scene root**
- **Y** = зафиксированный `sectionFloorY` (не поднимается при ARKit refinement)
- **XZ** — из transform выбранного якоря
- **Геометрию сетки не пересобирать** каждый кадр → freeze polygon snapshot после lock
- Точки контура — отдельно, на `ARAnchor`-корне (`IosFloorAnchorStore`)

### 4.6. Производительность

После lock:

- **Запретить** `asyncGridBuilder` для закреплённой плоскости
- Только toggle visibility + лёгкое обновление transform (без rebuild mesh)
- Raycast / hitTest в placement — **только для tap validation**, не для отрисовки сетки
- Не вызывать `enterPlacementHitOnlyMode` каждый кадр

---

## 5. Связка слоёв обратной связи

| Слой | Роль | Скорость |
|------|------|----------|
| Белая сетка scan-плоскости | «Где пол» | Стабильная, привязана к найденной поверхности |
| Зелёная/красная зона прицела | «Можно ли ставить точку» | Мгновенная, экранная |
| Зелёные точки контура | «Что уже размечено» | Стабильные (ARAnchor root) |
| Placement patch | **Убрать** (или fallback при `TRACKING_LOST`) | — |

---

## 6. Риски и митигация

| Риск | Митигация |
|------|-----------|
| Несколько белых сеток | `MAX_SURFACE_OVERLAYS = 1` + sticky anchor |
| Сетка уплывает с полом | World-pin + frozen Y + frozen geometry |
| Сетка прыгает при merge якорей | Sticky на section anchor; не переключаться на «большую» без причины |
| Сетка на столе рядом | Height band + classification filter |
| Фризы (`Delegate Hz` ≪ `1000/Camera gap`) | Не rebuild геометрию в placement; только visibility/transform |
| Точки плывут | `IosFloorAnchorStore`: root anchor + трёхуровневая коррекция + кнопка «Выровнять» |

---

## 7. Шаг 1 — диагностика на устройстве (перед фазой A)

**Статус:** готов к прогону на iPhone (debug build).

### Debug-поля (нижняя панель)

**Полный справочник всех полей:** [SURFACE_DETECTION.md § iOS Debug Panel](./SURFACE_DETECTION.md#ios-debug-panel--справочник-полей).

Кратко — метрики производительности для шага 1:

| Поле | Что значит |
|------|------------|
| **Phase** | `scan` / `placement` / `contour` — этап сессии |
| **Perf** | Авто-диагноз узкого места |
| **Delegate Hz** | Сколько раз в секунду завершился наш `didUpdateFrame` |
| **Camera gap** | Интервал между кадрами ARKit (`ARFrame.timestamp`) |
| **Frame work** | Сколько мс заняла обработка последнего `didUpdateFrame` |

### Как читать Perf

| Perf | Интерпретация |
|------|----------------|
| `ok (delegate ~Nhz)` | Камера и обработчик в норме |
| `handler heavy` | Обработчик тяжёлый, но ещё не критично |
| `handler blocked` | Обработчик блокирует поток (>250 ms на кадр) |
| `delegate blocked (camera ok)` | ARKit шлёт кадры (~30 Hz), но мы успеваем ~1 Hz |
| `delegate behind camera` | Кадры приходят чаще, чем мы их обрабатываем |
| `camera sparse` | Мало кадров от ARKit (трекинг / сессия) |
| `warming up` | Первый кадр, подождите 1–2 с |

### Чеклист прогона (записать значения)

**Scan (0 точек), 30 с на полу:**
- [ ] Phase = `scan`
- [ ] Camera gap ≈ ___ ms
- [ ] Delegate Hz ≈ ___
- [ ] Frame work ≈ ___ ms
- [ ] Perf = ___

**Placement (1+ точек), 30 с, двигать прицел:**
- [ ] Phase = `placement`
- [ ] Camera gap ≈ ___ ms
- [ ] Delegate Hz ≈ ___
- [ ] Frame work ≈ ___ ms
- [ ] Perf = ___
- [ ] Сетка отстаёт от прицела? да / нет

**Вывод перед фазой A:**
- Узкое место: camera / delegate handler / UX patch (записать одно)
- Если `delegate blocked` только в placement → фаза A + frozen geometry приоритетны
- Если `handler blocked` уже в scan → сначала облегчить `didUpdateFrame`, потом continuous floor

---

## 8. План реализации (фазы)

### Фаза A — Переключение режима (минимальный diff)

- [x] После 1-й точки: **не** вызывать `enterPlacementHitOnlyMode`
- [x] Продолжать `syncScanSurfaceVisualization` с `sectionFloorLocked = true`
- [x] Скрыть / не вызывать `syncPlacementPatch`
- [x] Вызвать `enterPlacementScanFreeze` + `pinCurrentOverlaysToWorldFloor` один раз при первой точке
- [x] Debug: `Plane renderer: continuous-floor` вместо `placement-patch`

### Фаза B — Sticky selection

- [x] Приоритет выбора: `sectionPlaneAnchorId` → `centerHit.anchor` → largest in band
- [x] Запретить `updateStickyFloor` переключать якорь после lock (уже было: early return)
- [x] `pickVisibleAnchors`: при `sectionFloorLocked` только один якорь из sticky id

### Фаза C — Frozen geometry

- [x] После lock: cancel `asyncGridBuilder` для закреплённой плоскости (skip rebuild в placement)
- [x] Не пересобирать mesh на `didUpdateNode` в placement (`placementScanFrozen`)
- [x] Transform pinned overlay: XZ от якоря, Y = `sectionFloorY`, **rotation = 0** (`overlayRotationFrozen`)

### Фаза D — Fallback и полировка

- [x] Если нет reference grid > 1.5 с: `syncPlacementPatch` fallback (`continuous-floor+fallback`, `Scan patch: fallback-on`)
- [x] Placement patch — только fallback, не primary
- [x] Debug: `Tap frame age`, защита тапа при stale frame / большом `Camera gap`

### Фаза E — Гибрид placement (reference + exploration)

- [x] Reference grid: одна frozen overlay (`continuous-floor`)
- [x] Exploration patch: `syncPlacementExplorationPatch` — сетка 1.5 m под прицелом в placement
- [x] Debug: `Plane renderer: continuous-floor+explore`, `Scan patch: explore-on`
- [x] Не влияет на sticky anchor / contour logic — только визуал

---

## 8.1. Фиксы стабильности (фазы 0–4, после тестов июнь 2026)

### Фаза 0 — Стабильность кадра

- [x] `lastProcessedFrame` из `didUpdateFrame`; тап и `applyEffects` используют его
- [x] Блокировка тапа: `Tap frame age > 150 ms` или `Camera gap > 200 ms`
- [x] Debug: `Tap frame age`, `Track quality`
- [x] `asyncGridBuilder` не запускается в placement frozen (`updatePolygonSurfaceGeometry` early return)
- [ ] Профилирование main thread (Instruments на Mac) — вне кода

### Фаза 1 — Точка под прицелом

- [x] Hit при тапе с `lastProcessedFrame` + свежий `resolvePlacementCenterHit`
- [x] `IosFloorAnchorStore`: `tapWorldPosition` заморожена в момент тапа
- [x] Блокировка тапа при `Hit age > 700 ms` в placement
- [x] Debug: `Tap Δ` (см) — расхождение preview hit и фактической точки

### Фаза 2 — Сетка не крутится после lock

- [x] `overlayRotationFrozen`: rotation = 0 после lock
- [x] Debounce transform 3 кадра после relocalization / `Camera gap ≥ 500 ms`

### Фаза 3 — Гибрид UX

- [x] Reference + exploration (см. фазу E)
- [x] Fallback patch при потере reference grid

### Фаза 4 — Полировка

- [x] `Camera gap ≥ 500 ms` или relocalizing → dim reference grid (`Track quality: degraded`)
- [x] Подсказка «Трекинг догоняет…» в placement при degraded
- [x] Debounce overlay transform после interruption / large gap
- [x] Критерии приёмки и debug-поля обновлены в [SURFACE_DETECTION.md](./SURFACE_DETECTION.md)

### Фаза 5 — Placement patch (render-driven) + стабильность точек

- [x] Explore patch в `renderer(updateAtTime:)` — ray ∩ `workingFloorY`, без `currentFrame` в render loop
- [x] World-aligned grid: `pg_create_world_aligned_placement_patch_grid_geometry`
- [x] One Euro Filter + fast-blend для patch (`PlacementPatchSmoother`)
- [x] `IosFloorAnchorStore`: root anchor, local XZ, политика коррекций — [ios-ar-point-stability.md](./ios-ar-point-stability.md)
- [x] Кнопка «Выровнять контур» для macro-сдвигов ≥ 8 см
- [x] Auto-small: авто-применение сдвигов 3–8 см после нестабильности трекинга
- [x] Debug: `Anchor corr` с `r:` и `d:` дельтами

---

## 9. Критерии приёмки (тест на устройстве)

1. После 1-й точки **остаётся одна** белая сетка пола (не несколько, не мигающие дубли)
2. Сетка **не отстаёт** от прицела на 2–3 секунды — она статична на плоскости, прицел показывает валидность
3. При движении камеры по комнате сетка **не уплывает** вертикально; контур **не дрейфует**
4. Точка ставится **под прицелом**, совпадает с визуальной зоной
5. `Delegate Hz` в placement сопоставим с `Camera gap` (не «delegate blocked»)
6. `Surface overlays: 1–2` в debug при placement (reference + optional explore patch)
7. При потере трекинга — понятное сообщение, без «зависшей» сетки в воздухе
8. У границ комнаты видна exploration-сетка под прицелом (`continuous-floor+explore`)
9. Тап отклоняется при `Tap frame age > 150 ms` или `Camera gap > 200 ms` — текст «Подождите, обновляется трекинг»
10. После фриза reference grid **не крутится** (rotation frozen)
11. При `Camera gap ≥ 500 ms` — `Track quality: degraded`, сетка тускнеет, подсказка «Трекинг догоняет…»
12. `Tap Δ` при нормальном тапе < 5 см
13. Потеря reference grid > 1.5 с → `continuous-floor+fallback`, `Scan patch: fallback-on`

---

## 10. Что НЕ делать

- Не развивать `placementPatchNode` как primary UX
- Не возвращать child-of-plane-anchor overlay без world-pin (даёт дрейф)
- Не показывать несколько overlays «для полноты картины»
- Не пересобирать polygon grid geometry каждый `didUpdateNode` в placement

---

## 11. Открытые вопросы (решить при реализации)

1. Переключать ли sticky anchor, если пользователь ушёл далеко и под прицелом **другая** плоскость того же уровня (ступенька, другая комната)?
2. Показывать ли reticle patch, когда overlay виден, или только цветной экранный прицел?
3. Нужен ли визуальный маркер первой точки (origin), как в Phase C старого плана?

---

*Фазы A–E, фиксы 0–4 и фаза 5 (patch + point stability) реализованы в коде. Delegate starvation — отдельно: [ios-ar-delegate-starvation-handoff-prompt.md](./ios-ar-delegate-starvation-handoff-prompt.md).*
