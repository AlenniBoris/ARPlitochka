# iOS AR: handoff prompt — ARKit delegate starvation (~10 s между кадрами)

> Исторический handoff prompt для отдельного расследования. Канонические AR-документы находятся в [`AR/`](../ar/README.md).

**Статус (июнь 2026):** проблема **не решена**. Частичный обход для placement explore patch: render loop `renderer(updateAtTime:)` + `workingFloorY` (см. [IOS_AR_CONTINUOUS_FLOOR_PLACEMENT_PLAN.md § Фаза 5](../archive/completed-plans/IOS_AR_CONTINUOUS_FLOOR_PLACEMENT_PLAN.md)). Стабильность **точек контура** — отдельно: [ios-ar-point-stability.md](../archive/completed-plans/ios-ar-point-stability.md).

Скопируй этот текст целиком и передай другой нейросети / разработчику.

---

## Задача

**iOS AR floor contour app — ARKit delegate starvation (~10 с между кадрами)**

### Проект

- **Repo:** Kotlin Multiplatform, ветка `feature/ios_ar_screen`
- **Платформа:** iOS, ARKit + SceneKit + Compose Multiplatform (`UIKitView` + `ARSCNView`)
- **Цель:** приложение для обводки пола в AR (как Android ARCore-версия): scan → первая точка → placement → контур

### Архитектура (ключевые файлы)

| Слой | Файл | Роль |
|------|------|------|
| Coordinator | `iosApp/src/iosMain/kotlin/com/example/arplitka/iosapp/platform/ar/IosArSessionCoordinator.kt` | `ARSessionDelegate` + `ARSCNViewDelegate`, весь frame loop |
| Raycast | `iosApp/src/iosMain/kotlin/com/example/arplitka/iosapp/platform/ar/IosArCenterRaycast.kt` | hitTest center, scan/placement hits |
| Plane viz | `iosApp/src/iosMain/kotlin/com/example/arplitka/iosapp/platform/render/IosArPlaneSurfaceRenderer.kt` | polygon grid overlays, reticle patch, placement explore patch |
| Async grid | `iosApp/src/iosMain/kotlin/com/example/arplitka/iosapp/platform/render/AsyncPolygonGridGeometryBuilder.kt` | polygon grid на background queue, apply на main |
| Contour | `iosApp/src/iosMain/kotlin/com/example/arplitka/iosapp/platform/render/IosArContourRenderer.kt` | точки/линии контура |
| Rules | `iosApp/src/iosMain/kotlin/com/example/arplitka/iosapp/presentation/support/IosArPlacementRules.kt` | throttling, debug metrics, perf diagnosis |
| UI | `iosApp/src/iosMain/kotlin/com/example/arplitka/iosapp/presentation/screen/IosArScreen.ios.kt` | Compose + debug panel |
| Domain | `shared/ar/domain/src/commonMain/kotlin/com/example/arplitka/shared/ar/domain/FloorArController.kt` | `onFrame` с `publishIfUiChanged` |
| Native bridge | `iosApp/src/nativeInterop/cinterop/plane_geometry_bridge.def` | C bridge для polygon/extent/grid |

Coordinator реализует **оба** delegate: `sceneView.delegate = this` и `sceneView.session.delegate = this`.

### Симптомы (воспроизводятся стабильно на устройстве)

**Три состояния по скриншотам debug panel:**

| Состояние | Phase | Camera gap | Delegate Hz | Frame work | Perf |
|-----------|-------|------------|-------------|------------|------|
| 1. Scan, до точки | scan | **~9931 ms** | **1** | 1 ms | `camera sparse (~0hz)` |
| 2. Тап (первая точка) | placement | **17 ms** | 1 | 1 ms | `delegate blocked (camera ok)` |
| 3. Placement, поворот камеры | placement | **~9831 ms** | 1 | 1–3 ms | `camera sparse (~0hz)` |

**UX:**

- После первой точки explore-patch под прицелом отстаёт от камеры на **3–11 секунд**
- Постоянно «Трекинг догоняет…» при degraded tracking
- Scan mode «иди и ищи поверхность» деградировал — плоскость есть, но обновления редкие
- **Пересканировать** — ИСПРАВЛЕНО: `Planes: 0` сразу после rescan (`publishScanResetUiSnapshot`)

### Диагноз (подтверждён метриками, не гипотеза)

**Проблема НЕ в тяжёлом handler'е `didUpdateFrame`.**  
`Frame work: 1–3 ms` — обработчик быстрый, когда delegate наконец срабатывает.

**Проблема в starvation `session(_:didUpdateFrame:)`.**  
ARKit отдаёт кадры delegate ~**1 раз в 10 секунд** (`Camera gap` ≈ timestamp delta между `ARFrame`).  
Всё, что завязано только на `didUpdateFrame` (reticle hit, explore-patch, scan metrics, `planeDebugMetrics`), замирает.

**Паттерн:**

- До точки и после поворота камеры в placement → gap ~10 с
- В момент тапа → gap **17 ms** (всплеск), `activatePlacementVisualizationImmediately()` срабатывает
- После поворота в placement → снова gap ~10 с, patch не двигается до следующего delegate tick

Debug panel читает из двух источников:

- `contourState` (Compose) — обновляется на tap через `onPointAdded`
- `planeDebugMetrics` — обновляется в `didUpdateFrame` → поэтому Camera gap / Phase / Placement в panel отстают

### Что уже пробовали (не решило проблему starvation)

1. **Placement fast path** — `handlePlacementDidUpdateFrame()` с early return, skip `didUpdateNode` в placement
2. **Live reticle** — `resolvePlacementLiveReticleHit()` (1 estimated hitTest)
3. **Tap immediate viz** — `activatePlacementVisualizationImmediately()` на tap — **работает в момент тапа**
4. **Scan throttle** — hitTest каждые 2 кадра, overlay budget, elevation, didUpdateNode каждый 3-й
5. **Scan hitTest-only** — без raycast в scan (`resolveScanCenterHit`)
6. **`renderer(updateAtTime:)` + `session.currentFrame`** — **убрано** из ранних экспериментов; **возвращено** только для placement patch (ray ∩ `workingFloorY`, без `currentFrame`) — не устраняет starvation delegate в scan
7. **Display link** — пробовали, убрали из-за contention
8. **Async polygon grid** — `AsyncPolygonGridGeometryBuilder`, completions на main queue
9. **Extent sync grid + SceneKit render tick без ARFrame** (коммит `e4eadd7`) — **откачен**, не помог
10. **Throttle Compose `onFrame`** в scan — частичные эксперименты
11. **Rescan fix** — `publishScanResetUiSnapshot`, `supportsSceneReconstructionMesh()` — **работает**

### Что работает

- Тап первой точки: мгновенно `Phase: placement`, `explore-on`, `Placement: valid`, gap 17 ms
- Rescan: `Planes: 0` сразу
- `Frame work` всегда низкий когда delegate fires

### Что НЕ работает (нужно решить)

- **Delegate Hz ~1** вместо 25–60
- **Camera gap ~9000+ ms** в scan и placement-after-camera-move
- Explore-patch не следует за камерой в placement (только на delegate ticks)
- Scan UX: поверхность видна, но «живость» обнаружения деградировала

### Подозреваемые блокеры main thread (не профилировались Instruments на устройстве)

- `AsyncPolygonGridGeometryBuilder` — apply `SCNGeometry` на main
- `didAddNode` / `didUpdateNode` → `syncPlaneOverlayOnNodeEvent` → `pg_geometry_signature` / `pg_polygon_area` / async grid request
- Compose `UIKitView` + SceneKit на одном main thread
- `floorArController.onFrame` → recomposition (частично дедупится `publishIfUiChanged`)
- Tracking degraded → overlay transform debounce churn
- Два delegate на одном NSObject

### Ограничения / антипаттерны

- **Не держать `ARFrame`** между вызовами
- **`session.currentFrame` в render loop** — ранее вызывало contention
- **Не предлагать только throttle / оптимизацию handler'а** — handler уже 1–3 ms, корень в том, что delegate не вызывается
- Нужен подход уровня **архитектуры**: почему ARKit не вызывает delegate 30 fps при живом SceneKit render?

### Критерии успеха (метрики на устройстве)

- `Delegate Hz`: **25–60** (не 1)
- `Camera gap`: **16–33 ms** (не 9000+)
- Scan: отзывчивое обнаружение поверхности при движении камеры
- Placement: explore-patch следует за прицелом без 10 с freeze
- Tap: по-прежнему мгновенный переход в placement
- Rescan: `Planes → 0` сразу (уже ок)

### Предлагаемые направления (для нового исследования)

1. **Instruments Time Profiler** на устройстве во время gap 10 с — что блокирует main queue?
2. Разделить ARSession delegate и SceneKit delegate на разные объекты?
3. Убрать / отложить async grid completions с main queue; sync extent grid только в scan?
4. Минимальный placement update path **без ARFrame** (screen-center ray ∩ floor plane через `pointOfView`) — но предыдущие попытки display link / render tick не помогли starvation delegate в scan
5. Проверить, не паузится ли session, нет ли deadlock между Compose UIKitView и ARSCNView
6. Сравнить с минимальным ARSCNView-only sample без Compose — воспроизводится ли gap?

### Сборка

```bash
./gradlew :iosApp:linkDebugFrameworkIosArm64
```

### Git

- Ветка: `feature/ios_ar_screen`
- HEAD после отката: `bbc95b8`
- Откатанный коммит (не помог): `e4eadd7` — extent grid + render tick

### Подтянуть на Mac после force push

```bash
cd /path/to/AndroidARPlitka
git fetch origin
git checkout feature/ios_ar_screen
git reset --hard origin/feature/ios_ar_screen
```

---

**Нужно найти root cause starvation `didUpdateFrame` и исправить так, чтобы delegate стабильно работал 30+ Hz. Патчи «обойти медленный delegate» без устранения starvation — недостаточны.**
