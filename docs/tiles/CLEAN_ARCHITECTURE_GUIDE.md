# Clean Architecture и обработка ошибок

Проект строго следует принципам Clean Architecture для обеспечения тестируемости и независимости слоев.

## Слои архитектуры

### 1. Domain Layer (Бизнес-логика)
*   **Модели**: Чистые Kotlin-классы (`Tile`, `TileColor`).
*   **Repository Interfaces**: Определяют контракты для получения данных.
*   **UseCases**: Содержат конкретные сценарии использования. 
    *   *Особенность*: Используется **паттерн Прокси (делегирование)** для UseCase без собственной логики:
        ```kotlin
        class GetTilesUseCase(repo: TilesRepository) : suspend () -> Result by repo::getTiles
        ```
*   **Exceptions**: Типизированные доменные ошибки, наследуемые от `Throwable`:
    ```kotlin
    sealed class TileException : Throwable() {
        object Network : TileException()
        object Server : TileException()
        // ...
    }
    ```

### 2. Data Layer (Данные)
*   **Имплементации репозиториев**: Реализуют интерфейсы из Domain.
*   **API (Ktor)**: Описание сетевых эндпоинтов.
*   **DTO**: Объекты для десериализации JSON.
*   **Обработка ошибок**: Использование `runCatching` внутри `withContext(Dispatchers.IO)` для безопасного выполнения запросов и маппинга исключений через `toTileException()`.

### 3. Presentation Layer (UI)
*   **ViewModel**: Работает с UseCase и получает результат в виде `CustomResultModelDomain`.
*   **UI Mappers**: Преобразуют `TileException` в строковые ресурсы или иконки для пользователя.

## Универсальный результат: `CustomResultModelDomain<T, E>`
Для передачи результата между слоями используется запечатанный класс с двумя дженериками:
*   `Success(data: T)`: Успешное выполнение.
*   `Error(exception: E)`: Ошибка типа `E` (например, `TileException`).

Это заставляет Presentation-слой явно обрабатывать оба сценария через конструкцию `when`, исключая "проглатывание" ошибок.
