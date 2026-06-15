# Паритет AR-Платформ

Этот документ описывает, как сравнивать и выравнивать AR-поведение Android и iOS.

## Референсная Политика

Android сейчас является продуктовым референсом для полного AR-flow:

- Поиск поверхностей.
- Постановка точек.
- Замыкание контура.
- Подтверждение / финализация.
- Заливка зоны.
- Toggle плитки.
- Смена типа плитки.
- Поворот текстуры.
- Reset / rescan.

iOS должен совпадать с Android по пользовательскому поведению, если нет платформенного ограничения или явного продуктового решения о расхождении.

## Shared И Platform-Specific

Shared KMP code должен владеть:

- AR UI state contracts.
- Events и reducers/controllers.
- Tile state.
- Чистой геометрической математикой там, где это возможно.
- Правилами видимости фаз.

Platform-specific code должен владеть:

- Конфигурацией ARCore/ARKit session.
- Native hit testing / raycasting.
- Anchors и world transforms.
- Rendering primitives и native bridge code.
- Политикой производительности платформы.

## Текущие Различия

| Область | Android | iOS |
|---------|---------|-----|
| Surface visualization | Нативный SceneView/ARCore `planeRenderer` | Кастомные SceneKit polygon grid overlays |
| Placement hit | ARCore `Frame.hitTest` + `Plane.isPoseInPolygon` | ARKit raycast / hit-test bridge |
| Стабильность контура | ARCore anchors / Android state | Root-anchor store + correction policy |
| Заливка плиткой | Local bounds-aware bitmap pattern | SceneKit/native bridge; parity work продолжается |
| UI shell | Android Compose | Compose Multiplatform + UIKitView |

## Правила Принятия Решений

Когда поведение отличается:

1. Сначала проверить, определяет ли Android уже продуктовый сценарий.
2. Если да, iOS должен соответствовать Android.
3. Если Android-поведение явно неверное, нужно обновить продуктово-инженерное решение и исправить обе платформы.
4. Не патчить одну платформу визуальными константами "чтобы было похоже", если такое правило не задокументировано.
5. Для геометрии и state transitions предпочитать shared pure math.

## Обязательные Данные Для AR-Изменений

Для любого нетривиального AR parity change нужно документировать:

- Затронутые платформы.
- Референсное поведение.
- Скриншоты или наблюдения с устройства.
- Релевантные debug fields.
- Code paths.
- Команды сборки/тестов.
- Оставшийся device QA.

## Связанные Документы

- [PLATFORM_SURFACE_DETECTION.md](./PLATFORM_SURFACE_DETECTION.md)
- [PLATFORM_CONTOUR_STABILITY.md](./PLATFORM_CONTOUR_STABILITY.md)
- [PLATFORM_TILE_FILLING.md](./PLATFORM_TILE_FILLING.md)
