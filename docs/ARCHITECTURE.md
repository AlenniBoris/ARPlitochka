# Архитектурные принципы

Проект следует принципам Clean Architecture и MVVM.

## Multiplatform
- Общие бизнес-сущности, DTO, use cases, mock contracts и UI shell проектируются как KMP/CMP-ready код.
- Android и iOS должны поддерживать одинаковые пользовательские сценарии.
- Новые пользовательские фичи проектируются shared-first: сначала общий contract/state/UI в KMP/CMP, затем platform-specific реализации только для SDK-зависимых частей.
- Android и iOS entry points должны запускать один `:shared:app` root. Разные корневые navigation shell для платформ запрещены.
- AR-движок является platform-specific: Android использует ARCore/SceneView, iOS использует ARKit/native interop.
- Общий AR-контракт хранится отдельно от platform rendering слоя.

## Слой Presentation

### UI (Compose)
- Большие экраны разбиваются на мелкие компоненты в пакете `presentation.components`.
- Сложная логика рендеринга (например, AR) выносится в отдельные слои/компоненты.

### ViewModel
- ViewModel оперирует только логическими состояниями (Enum, Sealed классы).
- Для управления сложными процессами (например, AR) используется явная стейт-машина этапов (`Workflow Stages`). Подробнее в [docs/ar/WORKFLOW_STAGES.md](ar/WORKFLOW_STAGES.md).
- **Запрещено** использовать Android-контекст или ресурсы напрямую во ViewModel.
- Вместо `UiText` используются мапперы в UI-слое.

### Mappers (`presentation.utils.mappers`)
- Используются для преобразования состояния из ViewModel в строковые ресурсы или другие UI-сущности.
- Реализуются как `@Composable` функции-расширения для Enums.
- Позволяют держать ViewModel чистой от логики отображения и зависимости от `R.string`.

## Слой Domain
- Содержит бизнес-логику, не зависящую от платформы.
- Модели данных (Data Classes) и интерфейсы репозиториев. Подробнее в [docs/tiles/DATA_MODELS.md](tiles/DATA_MODELS.md).
- Валидация данных (атомарная и агрегатная). Подробнее в [docs/VALIDATION_GUIDE.md](VALIDATION_GUIDE.md).

## Слой Data
- Реализация интерфейсов из Domain.
- Работа с ARCore, файловой системой или сетью.
- Сетевой слой и маппинг DTO. Подробнее в [docs/tiles/CLEAN_ARCHITECTURE_GUIDE.md](tiles/CLEAN_ARCHITECTURE_GUIDE.md).

## Тестирование
- Каждая новая функциональность должна покрываться Unit-тестами.
- Каждый новый экран должен иметь UI-тест на базе Kaspresso (Page Object паттерн).
- Мапперы UI-состояний тестируются в `androidTest`.

Подробные правила см. в [docs/TESTING.md](TESTING.md).
