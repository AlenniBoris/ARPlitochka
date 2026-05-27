# AR Plitka

Приложение для визуализации напольной плитки в дополненной реальности (AR).

## Архитектура
Проект построен на принципах **Clean Architecture** с использованием многомодульной структуры. Подробности см. в [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Модули
- `:app` — основной модуль приложения.
- `:features:floor-detection` — логика обнаружения пола и рендеринга плитки.
- `:features:catalog` — каталог доступной плитки.
- `:shared:ui:*` — общие UI-компоненты и навигация.
- `:shared:ar:core` — базовые утилиты AR.

Подробные правила модуляризации см. в [docs/MODULARIZATION.md](docs/MODULARIZATION.md).

## Технологии
- Kotlin, Coroutines, Flow
- Jetpack Compose
- ARCore, SceneView
- Hilt (Dependency Injection)
- Navigation Compose

## Документация
- [Архитектура](docs/ARCHITECTURE.md)
- [Модуляризация](docs/MODULARIZATION.md)
- [Навигация](docs/NAVIGATION.md)
- [Тестирование](docs/TESTING.md)
