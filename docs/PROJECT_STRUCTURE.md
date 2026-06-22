# Структура проекта AR Plitka

Проект построен на базе **Kotlin Multiplatform (KMP)** с использованием Clean Architecture и многомодульной структуры.

## Основные слои и модули

### 1. Платформенные модули (Верхний уровень)
Эти модули являются точками входа для конкретных платформ.
- **`:app`**: Android-приложение (Hilt, MainActivity, навигация через `hiltViewModel`).
- **`:iosApp`**: iOS-приложение (SwiftUI/Compose, MainViewController).
- **`:shared:app`**: Общая Compose-оболочка приложения (навигационный шелл, BottomBar).

### 2. Фичи (`:features:*`)
Каждая фича — это отдельный экран или логически завершенный функционал.
- **`:features:catalog`**: Экран каталога плитки. Содержит свою ViewModel и UI.
- **`:features:floor-detection`**: Логика обнаружения пола для AR.

### 3. Общие бизнес-модули (`:shared:*`)
Содержат бизнес-логику, общую для нескольких фич.
- **`:shared:tiles`**: Работа с данными плитки (Repository, UseCases, API).
- **`:shared:core`**: Базовая инфраструктура (Result модели, Exceptions, AppConfig, DataStore).
- **`:shared:ar:contracts`**: Интерфейсы и контракты для взаимодействия с AR.
- **`:shared:ar:core`**: Базовая логика AR.

### 4. UI модули (`:shared:ui:*`)
Общие UI компоненты и ресурсы.
- **`:shared:ui:core`**: Базовые UI утилиты, мапперы ошибок (`toUiString`).
- **`:shared:ui:kit`**: Библиотека готовых UI компонентов (кнопки, карточки, индикаторы).
- **`:shared:ui:navigation`**: Общие определения маршрутов навигации.

### 5. Сетевой слой и Моки
- **`:network:core`**: Настройка Ktor HttpClient и базовые перехватчики.
- **`:mock:core`**: Ядро системы мокирования (AssetReader, RouteRegistry).
- **`:mock:tiles`**: Конкретные моки для данных плитки.

## Как найти нужный код?

- **Экраны и UI логика**: Ищите в `features/<name>/src/commonMain/kotlin/.../presentation`.
- **Бизнес-логика (UseCases)**: Ищите в `shared/<name>/src/commonMain/kotlin/.../domain/usecase`.
- **Работа с данными (Repository/API)**: Ищите в `shared/<name>/src/commonMain/kotlin/.../data`.
- **Общие модели и ошибки**: Ищите в `shared/core/src/commonMain/kotlin/.../domain/model`.
- **Общие UI компоненты**: Ищите в `shared/ui/kit/src/commonMain/kotlin/...`.

## Правила взаимодействия
1. **Фичи не зависят друг от друга**. Если нужен общий функционал, он выносится в `:shared:*`.
2. **`:shared:app`** знает о фичах только через интерфейсы или лямбды, передаваемые из платформенных модулей.
3. **DI**: На Android используется **Hilt**, на iOS — ручное внедрение через **Components** (например, `TilesComponent`).
