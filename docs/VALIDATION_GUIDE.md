# Руководство по валидации данных

В проекте используется типизированная система валидации, основанная на функциональных принципах. Это позволяет разделять логику проверки данных и их использование.

## Основные концепции

### 1. `Validated<F, A>`
Тип-контейнер для результата валидации:
- `Valid(value: A)` — данные успешно прошли проверку.
- `Invalid(failure: F)` — данные невалидны, содержит объект ошибки `F`.

### 2. `Validate<F, A>`
Функциональный интерфейс (SAM), представляющий собой функцию `(A) -> Validated<F, A>`.

---

## Уровни валидации

### 1. Атомарная валидация (Atomic)
Проверка одного конкретного правила для одного поля. Атомарные валидаторы максимально просты и переиспользуемы.

**Пример (HasHttpOrHttpsPrefixValidator.kt):**
```kotlin
fun hasHttpOrHttpsPrefixValidator(
    failure: ImageUrlValidationError = ImageUrlValidationError.NOT_HTTP_OR_HTTPS,
): Validate<ImageUrlValidationError, String> =
    validate(failure) { url ->
        url.startsWith("http://") || url.startsWith("https://")
    }
```

### 2. Агрегатная валидация (Aggregate / Composite)
Композиция нескольких атомарных валидаторов для проверки сложной сущности или набора правил.

**Пример построения агрегата:**
```kotlin
// В будущем здесь будет DSL для объединения валидаторов
fun validateTile(tile: Tile): Validated<List<TileError>, Tile> {
    // Пример ручного объединения
    val nameResult = nameValidator(tile.name)
    val priceResult = priceValidator(tile.basePrice)
    
    return if (nameResult is Valid && priceResult is Valid) {
        valid(tile)
    } else {
        // Сбор всех ошибок
        invalid(listOfNotNull(
            (nameResult as? Invalid)?.failure,
            (priceResult as? Invalid)?.failure
        ))
    }
}
```

---

## Где располагать валидаторы?

1.  **Инфраструктура**: Общие типы `Validated` и `Validate` лежат в `:shared:validation`.
2.  **Доменная логика**: Конкретные валидаторы лежат в доменном слое модуля:
    - `shared/<module>/src/commonMain/kotlin/.../domain/validation/atomic/`
    - `shared/<module>/src/commonMain/kotlin/.../domain/validation/aggregate/`

## Почему это важно?
- **Тестируемость**: Атомарные валидаторы легко проверять Unit-тестами.
- **Чистота домена**: Валидация происходит до того, как данные попадут в бизнес-логику.
- **UI-Feedback**: Типизированные ошибки (`Invalid<F>`) позволяют легко маппить их в понятные сообщения для пользователя.
