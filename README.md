# AR Plitka

Приложение для визуализации напольной плитки в дополненной реальности (AR).

## Архитектура
Проект построен на принципах **Clean Architecture** с использованием многомодульной структуры. Подробности см. в [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Модули
- `:app` — основной модуль приложения.
- `:iosApp` — iOS entry point и Compose Multiplatform host.
- `:features:floor-detection` — логика обнаружения пола и рендеринга плитки.
- `:features:catalog` — каталог доступной плитки.
- `:network:core` — KMP-ready сетевой фундамент.
- `:mock:*` — KMP-ready mock-инфраструктура и mock assets для debug/UI-тестов.
- `:shared:app` — общий Compose Multiplatform app shell.
- `:shared:ar:contracts` — общие AR state/event/geometry contracts.
- `:shared:tiles` — общий KMP-ready модуль плитки.
- `:shared:ui:*` — общие UI-компоненты и навигация.
- `:shared:ar:core` — базовые утилиты AR.

Подробные правила модуляризации см. в [docs/MODULARIZATION.md](docs/MODULARIZATION.md).

## Технологии
- Kotlin, Coroutines, Flow
- Kotlin Multiplatform foundation для shared/domain/network/mock слоев
- Compose Multiplatform для общего app shell
- Jetpack Compose
- ARCore, SceneView, ARKit interop для iOS
- Hilt (Dependency Injection)
- Navigation Compose

## Документация
- [Архитектура](docs/ARCHITECTURE.md)
- [Модуляризация](docs/MODULARIZATION.md)
- [Навигация](docs/NAVIGATION.md)
- [Тестирование](docs/TESTING.md)
- [Backend и моки](docs/BACKEND_MOCKING_PLAN.md)
