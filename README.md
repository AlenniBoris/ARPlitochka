# AR Plitka

Приложение для визуализации напольной плитки в дополненной реальности (AR).

## Архитектура
Проект построен на принципах **Clean Architecture** с использованием многомодульной структуры. Подробности см. в [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Модули
- `:app` — основной модуль Android-приложения.
- `:iosApp` — точка входа iOS и хост для Compose Multiplatform.
- `:features:floor-detection` — логика обнаружения пола и рендеринга плитки.
- `:features:catalog` — каталог доступной плитки.
- `:network:core` — общий сетевой фундамент, совместимый с KMP.
- `:mock:*` — инфраструктура моков и ассеты для разработки и UI-тестов.
- `:shared:app` — общая оболочка приложения на Compose Multiplatform.
- `:shared:ar:contracts` — общие контракты состояния, событий и геометрии AR.
- `:shared:tiles` — общий бизнес-модуль плитки.
- `:shared:ui:*` — общие UI-компоненты и навигация.
- `:shared:ar:core` — базовые утилиты AR.

Подробные правила модуляризации см. в [docs/MODULARIZATION.md](docs/MODULARIZATION.md).

## Технологии
- Kotlin, Coroutines, Flow
- Kotlin Multiplatform для общих слоев (domain/data/network/mock)
- Compose Multiplatform для общей оболочки приложения
- Jetpack Compose
- ARCore, SceneView, ARKit (через native interop для iOS)
- Koin (Dependency Injection)
- Navigation Compose

## Документация
- [Архитектура](docs/ARCHITECTURE.md)
- [Модуляризация](docs/MODULARIZATION.md)
- [Навигация](docs/NAVIGATION.md)
- [Тестирование](docs/TESTING.md)
- [Backend и моки](docs/BACKEND_MOCKING_PLAN.md)
- [Валидация данных](docs/VALIDATION_GUIDE.md)

### Дополненная реальность (AR)
- [Поиск поверхностей](docs/ar/PLATFORM_SURFACE_DETECTION.md) — стратегии обнаружения пола.
- [Стабильность контура](docs/ar/PLATFORM_CONTOUR_STABILITY.md) — якоря, коррекции и выравнивание.
- [Заливка плиткой](docs/ar/PLATFORM_TILE_FILLING.md) — алгоритмы наложения текстур.
- [Паритет платформ](docs/ar/PLATFORM_PARITY.md) — сравнение Android и iOS реализаций.
- [Ограничения](docs/ar/LIMITATIONS_LARGE_ZONES.md) — особенности работы на больших площадях.
