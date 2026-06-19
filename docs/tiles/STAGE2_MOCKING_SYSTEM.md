# Техническая реализация системы мокирования (Этап 2)

Этот документ описывает архитектуру и техническую реализацию системы мокирования в KMP-проекте.

## 1. Управление конфигурацией (`AppConfig`)

Вместо локальных флагов используется глобальное состояние конфигурации, хранящееся в модуле **`:shared:core`**.

### `AppConfig` (Data Class)
```kotlin
data class AppConfig(
    val isMockEnabled: Boolean = true,
    val apiBaseUrl: String = "https://api.example.com",
    val mockDelayMs: Long = 500
)
```

### Реактивное управление (Сегрегированные UseCase)
Для соблюдения принципа единственной ответственности (SRP) используются отдельные UseCase в `:shared:core`:
*   **`GetAppConfigUseCase`**: Возвращает `StateFlow<AppConfig>` для наблюдения за состоянием.
*   **`SetMockEnabledUseCase`**: Переключает флаг мокирования.
*   **`SetMockDelayUseCase`**: Устанавливает задержку ответов.

### Персистентность (Jetpack DataStore)
Настройки сохраняются между перезапусками приложения с помощью **Jetpack DataStore (KMP)**:
*   **Android**: Реализация через `createAndroidDataStore(context)`. Используется `applicationContext` для предотвращения утечек памяти.
*   **iOS**: Реализация через `createIosDataStore()`, использующая `NSDocumentDirectory`.
*   **Константы**: Путь к файлу централизован в константе `AppConfigManager.DATASTORE_FILE_NAME`.

### Безопасность релиза
В `AppConfigManager` реализована логика принудительного сброса:
*   При инициализации проверяется флаг `isReleaseBuild`.
*   В релизных сборках `isMockEnabled` всегда устанавливается в `false`, независимо от сохраненных данных.
*   Метод `updateConfig` блокирует попытки включения моков в релизе.

## 2. Чтение ресурсов (AssetReader)

Для доступа к JSON-файлам используется механизм `expect/actual` в модуле **`:mock:core`**:
*   **Android**: Использует `context.assets.open(path)`.
*   **iOS**: Использует `NSBundle.mainBundle` для поиска ресурсов в бандле.
*   **Безопасность**: На Android `AssetReader` инициализируется через `applicationContext`, что исключает утечки Activity.

## 3. Реестр маршрутов (`MockRouteRegistry`)

`MockRouteRegistry` — это централизованный справочник, связывающий URL-пути с моковыми ассетами.

### Принцип работы:
1.  **Регистрация**: Бизнес-модули регистрируют свои пути через DSL:
    ```kotlin
    "/tiles" reply JsonAsset("mock/tiles/catalog/all.json")
    ```
2.  **Поиск**: Поддерживает частичное совпадение URL (через `endsWith`), что позволяет находить ассеты даже если запрос содержит полный домен или динамические параметры.

## 4. Механизм перехвата (MockInterceptor)

В `HttpClient` (модуль `:network:core`) внедряется плагин, работающий по алгоритму:
1.  Проверка `AppConfig.isMockEnabled`.
2.  Если моки включены, получение пути из `request.url`.
3.  Поиск пути в `MockRouteRegistry`.
4.  Если ассет найден:
    *   Применение задержки `AppConfig.mockDelayMs`.
    *   Чтение файла через `AssetReader`.
    *   Возврат `HttpStatusCode.OK` с телом JSON.
5.  Если моки выключены или путь не найден — выполнение реального сетевого запроса.

## 5. Обработка ошибок и результатов

Внедрена унифицированная система в модуле **`:shared:core`**:
*   **`CustomResultModelDomain<T, E>`**: Обертка для результатов (Success/Error).
*   **`CommonException`**: Sealed interface для доменных ошибок. Состоит из `data object` (Connection, Server, Client, Serialization, Unknown) без внутренних свойств.
*   **Маппинг исключений**: Реализован в доменном слое фич (например, `ExceptionMapper.kt` в `:shared:tiles`) через расширение `Throwable.toCommonException()`.
*   **UI-маппинг**: Реализован в слое презентации (например, `UiExceptionMapper.kt` в `:shared:app`) через расширение `CommonException.toUiString()`.

## 6. Тестирование

Ключевые компоненты покрыты Unit-тестами в `commonTest`:
*   `TileDtoMapperTest` — проверка маппинга данных.
*   `AppConfigManagerTest` — проверка реактивности, сохранения и безопасности релиза.
*   `MockRouteRegistryTest` — проверка регистрации и поиска (включая частичные совпадения).
*   `TilesRepositoryImplTest` — проверка интеграции репозитория с системой обработки ошибок.
