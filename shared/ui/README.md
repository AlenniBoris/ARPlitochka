# Module `:shared:ui`

Shared-модуль для общих Compose UI-компонентов и UI-friendly моделей.

## Назначение

`shared:ui` содержит переиспользуемые элементы интерфейса, которые не завязаны на конкретный feature-модуль. Сейчас он используется `:app` и `:features:floor-detection`.

Модуль помогает не дублировать базовые UI-компоненты и держать feature-модули чище.

## Структура

```text
shared/ui/
├── build.gradle.kts
└── src/main/java/com/example/arplitka/shared/ui/
    ├── CommonUi.kt
    └── UiText.kt
```

## Основные Файлы

### `CommonUi.kt`

Содержит общие Compose-компоненты:

- `CenterReticle`;
- `StatusPanel`;
- `BlockingMessage`;
- `DebugPanel`.

### `UiText.kt`

Содержит sealed class `UiText`, который позволяет хранить текст UI как:

- динамическую строку;
- ссылку на string resource с аргументами.

Это удобно для ViewModel: она может отдавать `UiText.StringResource(...)`, не превращая состояние в обычные hardcoded-строки.

## Компоненты

### `CenterReticle`

Центральный прицел для AR-сценария.

Используется, чтобы показать пользователю точку, по которой выполняется hit-test на полу. Поддерживает активное и неактивное состояние.

### `StatusPanel`

Верхняя панель статуса и инструкции.

Используется для сообщений вроде:

- идет поиск пола;
- пол найден;
- потерян tracking;
- нужно двигать устройство.

### `BlockingMessage`

Экран-заглушка для блокирующих состояний:

- нет разрешения камеры;
- ARCore не поддерживается;
- AR-сессия не запустилась.

Может содержать action-кнопку.

### `DebugPanel`

Небольшая панель key-value значений для debug-информации.

В `floor-detection` используется только в debug-сборке.

## Использованные Технологии

- Kotlin;
- Jetpack Compose;
- Compose Canvas;
- Material 3;
- Android string resources через `stringResource`;
- `Context.getString` для non-composable преобразования `UiText`.

## Зависимости Модуля

Основные зависимости:

- AndroidX Core KTX;
- Compose BOM;
- Compose UI;
- Compose UI Graphics;
- Material 3;
- Compose tooling preview.

## Границы Ответственности

В этот модуль можно добавлять:

- общие Compose-компоненты;
- простые UI-модели;
- theme-independent визуальные элементы;
- компоненты, которые могут использоваться несколькими модулями.

В этот модуль не стоит добавлять:

- ARCore-specific логику;
- ViewModel конкретной фичи;
- repository/use case;
- сетевые DTO;
- бизнес-логику расчета плитки;
- компоненты, которые используются только в одном месте и сильно завязаны на конкретную feature-модель.

## Когда Выносить UI Сюда

Компонент стоит переносить в `:shared:ui`, если:

- он не зависит от конкретного `FloorUiState`;
- принимает простые параметры;
- может пригодиться на других экранах;
- не содержит feature-specific действий.

Если компонент знает о точках контура, плитке, AR-сессии или состоянии пола, ему лучше оставаться в `:features:floor-detection`.
