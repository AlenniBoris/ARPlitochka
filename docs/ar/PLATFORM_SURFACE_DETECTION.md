# Поиск Поверхностей В AR

Этот документ — канонический контракт поиска горизонтальных поверхностей на Android и iOS.

## Цель Продукта

Пользователь должен видеть полезные горизонтальные поверхности во время сканирования и иметь возможность ставить точки контура только на валидную поверхность под центральным прицелом.

Общие UX-правила:

- Поверхности горизонтальные: пол, стол, верх кровати, уличная плитка и т.д.
- Центральный прицел определяет, можно ли поставить точку.
- Минимальная валидная площадь поверхности — `0.15 m²`.
- Scan-визуализация — это обратная связь; постановка точки всё равно зависит от confirmed center hit.
- После перехода в contour mode визуализация surface скрывается, чтобы сохранить стабильность AR-сцены.

## Android

Android использует ARCore и SceneView.

Референсные файлы:

- `features/floor-detection/.../FloorArViewModel.kt`
- `features/floor-detection/.../ArSceneLayer.kt`
- `features/floor-detection/.../FloorModels.kt`

Поведение:

- Включён поиск горизонтальных плоскостей ARCore.
- Нативный SceneView `planeRenderer` показывает горизонтальные plane.
- Постановка точки использует center hit и `Plane.isPoseInPolygon(hitPose)`.
- Surface не фиксируется на первом найденном полу. Пользователь может перевести прицел на другую горизонтальную плоскость.
- Android остаётся продуктовым референсом для полного flow контура и плитки.

## iOS

iOS использует ARKit, `ARSCNView` и кастомный SceneKit-рендеринг.

Референсные файлы:

- `iosApp/.../IosArSessionCoordinator.kt`
- `iosApp/.../IosArCenterRaycast.kt`
- `iosApp/.../IosArPlaneSurfaceRenderer.kt`
- `iosApp/.../IosArContourRenderer.kt`
- `iosApp/src/nativeInterop/cinterop/plane_geometry_bridge.def`

Поведение:

- Включён поиск горизонтальных plane через ARKit.
- Scan-визуализация использует polygon grid overlays для `ARPlaneAnchor`.
- Center hit использует native raycast / hit-test bridge paths.
- Постановка точки требует confirmed surface hit.
- В contour mode scan overlays скрываются (`contour-hidden`), чтобы избежать просадок FPS и визуального шума.

## Общие Правила Видимости

| Фаза | Android | iOS |
|------|---------|-----|
| Scan | Surface renderer видим | Surface grid видим |
| Placement | Точки/линии контура видимы | Точки/линии контура видимы |
| Closed contour | Заливка зоны видима | Заливка зоны видима |
| Finalized | Заливка остаётся; доступны tile controls | Заливка остаётся; доступны tile controls |
| Tile mode | Точки/линии скрыты, текстура видима | Точки/линии скрыты, текстура видима |

## Debug-Поля

Важные поля iOS debug-панели:

- `Plane renderer`: режим scan/contour renderer.
- `Hit path`: источник center hit.
- `Detect gate`: подтверждён ли hit и валиден ли он.
- `Scan patch`: состояние reticle patch.
- `Placement`: состояние placement (`valid`, `stale`, `height` и т.д.).
- `Largest plane`: площадь крупнейшей найденной plane.
- `Cull`: счётчики скрытых/cull overlays.

## Связанные Исторические Документы

- `docs/archive/completed-plans/SURFACE_DETECTION.md`
- `docs/archive/completed-plans/IOS_AR_SURFACE_STRATEGY.md`
- `docs/archive/completed-plans/IOS_AR_CONTINUOUS_FLOOR_PLACEMENT_PLAN.md`
