# Правила модуляризации

В проекте используется многомодульная архитектура для обеспечения масштабируемости и разделения ответственности.

## Типы модулей

### 1. App Модуль (`:app`)
Точка входа в приложение. Содержит `MainActivity`, настройку навигации и внедрение зависимостей (Hilt).

### 1.1 iOS App Модуль (`:iosApp`)
Точка входа для iOS. Собирает framework `ARPlitkaIos`, экспортирует `MainViewController()` для SwiftUI/Xcode и подключает общий Compose Multiplatform root.

### 2. Feature Модули (`:features:*`)
Содержат логику конкретных экранов или функций.
- Название: `:features:<feature-name>`
- Структура:
    - `domain`: Модели, репозитории (интерфейсы), Use Cases.
    - `data`: Реализация репозиториев, API, БД.
    - `presentation`: UI (Compose), ViewModel, Mappers, Utils.
    - `test`: Unit-тесты для ViewModel, Use Cases и Utils.
    - `androidTest`: UI-тесты для экранов (Kaspresso) и тесты мапперов.

### 3. Shared Модули (`:shared:*`)
Переиспользуемый код, разбитый на микро-модули.
- `:shared:ui:core` — базовые UI-сущности, темы.
- `:shared:ui:navigation` — компоненты навигации (BottomBar).
- `:shared:ui:kit` — набор общих UI-компонентов (Reticle, Panels).
- `:shared:ar:core` — базовые утилиты для работы с ARCore и SceneView.
- `:shared:ar:contracts` — KMP-ready AR-сценарии, state/event/command contracts и нормализация геометрии без ARCore/ARKit зависимостей.
- `:shared:app` — Compose Multiplatform app layer: общий каталог, общий navigation shell, transition flow, bottom bar и слот для platform-specific AR.
- `:shared:tiles` — KMP-ready общий бизнес-модуль плитки: модели, DTO, repository, use case, API contract.

### 4. Network Модули (`:network:*`)
Технический сетевой фундамент.
- `:network:core` — KMP-ready common primitives (`ApiResult`, `NetworkError`, `NetworkConfig`) и будущая Ktor/Ktorfit инфраструктура.
- Network-модули не содержат бизнес endpoint'ов.
- Бизнес API живут в feature-модулях или shared business modules.

### 5. Mock Модули (`:mock:*`)
Моки для debug-разработки и UI-тестов.
- `:mock:core` — общий mock DSL, `JsonAsset`, routes registry.
- `:mock:tiles` — mock routes и assets плитки.
- Feature-модули не зависят от mock-модулей напрямую.

## Правила именования
- Все модули пишутся в нижнем регистре через дефис.
- Пакеты внутри модулей должны соответствовать структуре: `com.example.arplitka.<module-type>.<module-name>`.

## Зависимости
- Feature модули могут зависеть от Shared модулей.
- Feature модули НЕ должны зависеть друг от друга.
- App модуль зависит от всех Feature и необходимых Shared модулей.
- Debug-сборка `:app` может зависеть от `:mock:*`.
- Common code в KMP-ready модулях не должен зависеть от Android-only API.
- Platform-specific AR живет в platform source sets: Android использует ARCore/SceneView, iOS использует ARKit/native interop.
- Platform entry points (`:app`, `:iosApp`) не создают собственный продуктовый navigation shell. Они подключают `:shared:app` и передают platform-specific AR content.
