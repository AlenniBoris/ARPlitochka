# Тестирование

В проекте используются следующие подходы и инструменты для тестирования:

## Инструменты

- **JUnit 4**: Основной фреймворк для Unit-тестов.
- **MockK**: Библиотека для создания моков и стабов.
- **Turbine**: Библиотека для удобного тестирования Kotlin Coroutines Flow.
- **Kotlinx Coroutines Test**: Утилиты для тестирования корутин (диспетчеры, `runTest`).
- **Kaspresso**: Фреймворк для UI-тестирования, расширяющий Espresso и Kakao, с поддержкой Jetpack Compose.

## Структура тестов

### Unit-тесты (`src/test`)
Располагаются в папке `src/test` соответствующего модуля.
- **ViewModel**: Тестирование логики изменения состояния (`uiState`) в ответ на действия пользователя или события системы.
- **UseCase**: Тестирование бизнес-логики.
- **Utils**: Тестирование чистых функций (геометрия, расчеты).

### UI-тесты (`src/androidTest`)
Располагаются в папке `src/androidTest` соответствующего модуля.
- **Screen Tests**: Тестирование отображения компонентов и взаимодействия с ними с использованием Kaspresso.
- **Mappers**: Тестирование `@Composable` функций-мапперов, требующих Compose окружения.

## Правила написания тестов

1. **Именование**: Тесты именуются по принципу `имяФункции_условие_ожидаемыйРезультат` или с использованием обратных кавычек для описательных имен на английском (например, `` `initial state is correct` ``).
2. **Изоляция**: Каждый тест должен быть независимым. Используйте `mockk` для изоляции тестируемого компонента от его зависимостей.
3. **Flow**: Для тестирования `StateFlow` во ViewModel используйте `turbine`.
4. **UI**: Для UI-тестов используйте паттерн **Page Object** (в Kaspresso это `ComposeScreen`).
5. **Импорты**: Избегайте использования полных путей (fully qualified names) в коде тестов. Используйте импорты для упрощения читаемости.
6. **Обязательность**: Любая новая логика в `domain` или `presentation` (ViewModel) должна сопровождаться Unit-тестами. Новые экраны должны иметь хотя бы один базовый UI-тест.

### Shared AR domain (`:shared:ar:domain`)

KMP unit-тесты в `shared/ar/domain/src/commonTest` — общий контракт для iOS и будущей миграции Android:

| Тест | Что проверяет |
|------|----------------|
| `FloorArControllerTileTest` | `ToggleTileVisibility`, `RotateTexture`, `ChangeTileType`, reset tile fields |
| `FloorContourUiStateTileVisibilityTest` | `showContourPoints` / `showContourLines` / `showSectionFill` в tile mode |
| `FloorContourUiPublishTest` | UI publish snapshot при смене tile-полей |

Запуск (Windows / CI):

```bat
.\gradlew.bat :shared:ar:domain:testDebugUnitTest
```

Документация по плитке на iOS: [ios-tile-placement.md](./ios-tile-placement.md).

## Примеры

### Unit-тест ViewModel
```kotlin
@Test
fun `rotateTexture cycles through rotations`() = runTest {
    viewModel.uiState.test {
        awaitItem() // Пропуск начального состояния
        viewModel.rotateTexture()
        assertEquals(TextureRotation.DEGREES_45, awaitItem().textureRotation)
    }
}
```

### UI-тест Kaspresso
```kotlin
@Test
fun catalogScreen_displaysPlaceholderText() = run {
    step("Set up CatalogScreen") {
        composeTestRule.setContent { CatalogScreen() }
    }
    step("Check text") {
        onComposeScreen<CatalogScreenKaspresso>(composeTestRule) {
            placeholderText {
                assertIsDisplayed()
                assertTextEquals("Экран Каталога (Заглушка)")
            }
        }
    }
}
```
