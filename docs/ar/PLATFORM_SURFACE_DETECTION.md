# Поиск поверхностей в AR

Этот документ — канонический контракт поиска горизонтальных поверхностей на Android и iOS.

## Цель продукта

Пользователь должен видеть полезные горизонтальные поверхности во время сканирования и иметь возможность ставить точки контура только на валидную поверхность под центральным прицелом.

Общие UX-правила:

- Поверхности горизонтальные: пол, стол, верх кровати, уличная плитка и т.д.
- Центральный прицел определяет, можно ли поставить точку.
- Минимальная валидная площадь поверхности — `0.15 м²`.
- Визуализация сканирования — это обратная связь; постановка точки всё равно зависит от подтвержденного попадания в центр (confirmed center hit).
- После перехода в режим контура (contour mode) визуализация поверхности скрывается, чтобы сохранить стабильность AR-сцены.

## Android

Android использует ARCore и SceneView.

Референсные файлы:

- `features/floor-detection/.../FloorArViewModel.kt`
- `features/floor-detection/.../ArSceneLayer.kt`
- `features/floor-detection/.../FloorModels.kt`

Поведение:

- Включён поиск горизонтальных плоскостей ARCore.
- Нативный SceneView `planeRenderer` показывает горизонтальные плоскости (plane).
- Постановка точки использует попадание в центр (center hit) и `Plane.isPoseInPolygon(hitPose)`.
- Поверхность не фиксируется на первом найденном полу. Пользователь может перевести прицел на другую горизонтальную плоскость.
- Android остаётся продуктовым референсом для полного процесса (flow) контура и плитки.

## iOS

iOS использует ARKit, `ARSCNView` и кастомный SceneKit-рендеринг.

Референсные файлы:

- `iosApp/.../IosArSessionCoordinator.kt`
- `iosApp/.../IosArCenterRaycast.kt`
- `iosApp/.../IosArPlaneSurfaceRenderer.kt`
- `iosApp/.../IosArContourRenderer.kt`
- `iosApp/src/nativeInterop/cinterop/plane_geometry_bridge.def`

Поведение:

- Включён поиск горизонтальных плоскостей (plane) через ARKit.
- Визуализация сканирования использует наложение полигональной сетки (polygon grid overlays) для `ARPlaneAnchor`.
- Попадание в центр (center hit) использует нативный raycast / hit-test через мост (bridge).
- Постановка точки требует подтвержденного попадания в поверхность (confirmed surface hit).
- В режиме контура (contour mode) наложения сканирования скрываются (`contour-hidden`), чтобы избежать просадок FPS и визуального шума.

## Общие правила видимости

| Фаза | Android | iOS |
|------|---------|-----|
| Scan | Рендерер поверхности видим | Сетка поверхности видима |
| Placement | Точки/линии контура видимы | Точки/линии контура видимы |
| Closed contour | Заливка зоны видима | Заливка зоны видима |
| Finalized | Заливка остаётся; доступны элементы управления плиткой | Заливка остаётся; доступны элементы управления плиткой |
| Tile mode | Точки/линии скрыты, текстура видима | Точки/линии скрыты, текстура видима |

## Поля отладки (Debug)

Важные поля iOS debug-панели:

- `Plane renderer`: режим scan/contour renderer.
- `Hit path`: источник попадания в центр (center hit).
- `Detect gate`: подтверждён ли hit и валиден ли он.
- `Scan patch`: состояние reticle patch.
- `Placement`: состояние размещения (`valid`, `stale`, `height` и т.д.).
- `Largest plane`: площадь крупнейшей найденной плоскости.
- `Cull`: счётчики скрытых/отсеченных (cull) наложений.

## Связанные исторические документы

- `docs/archive/completed-plans/SURFACE_DETECTION.md`
- `docs/archive/completed-plans/IOS_AR_SURFACE_STRATEGY.md`
- `docs/archive/completed-plans/IOS_AR_CONTINUOUS_FLOOR_PLACEMENT_PLAN.md`
