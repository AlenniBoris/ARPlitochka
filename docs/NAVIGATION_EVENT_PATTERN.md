# Паттерн Навигации и Одноразовых Событий (Navigation & Event Pattern)

В проекте AR Plitka используется стандартизированный подход к разделению ответственности между UI, бизнес-логикой и навигацией.

## Основные компоненты

### 1. Screen (Уровень навигации и связи)
`Screen` — это точка входа для экрана. Он отвечает за:
- Получение `Navigator` (интерфейс `AppNavigator`).
- Инициализацию `ViewModel`.
- Подписку на одноразовые события (`ViewModel.event`).
- Вызов методов `Navigator` в ответ на события.
- Передачу данных и UI-колбэков в `Content`.

**Пример:**
```kotlin
@Composable
fun TileDetailsScreen(
    tileId: Long,
    navigator: AppNavigator,
    viewModel: TileDetailsViewModel = koinViewModel(parameters = { parametersOf(tileId) })
) {
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is TileDetailsEvent.OpenAr -> navigator.openAr()
                is TileDetailsEvent.NavigateBack -> navigator.back()
            }
        }
    }

    TileDetailsContent(
        state = viewModel.state.collectAsState().value,
        onBack = viewModel::onBack,
        onTryInAr = viewModel::onTryInAr
    )
}
```

### 2. Content (Уровень чистого UI)
`Content` — это приватная Composable-функция, которая:
- Не знает о существовании `ViewModel` или `Navigator`.
- Принимает только состояние (`UiState`) и простые лямбда-колбэки (`() -> Unit`).
- Максимально проста для тестирования и превью.

### 3. ViewModel (Уровень логики и событий)
`ViewModel` управляет состоянием и эмитит одноразовые события через `SingleFlowEvent`.
- `state`: `StateFlow<UiState>` для постоянного состояния экрана.
- `event`: `Flow<Event>` для одноразовых действий (навигация, алерты).

### 4. SingleFlowEvent
Утилита в `:shared:core`, обеспечивающая надежную доставку одноразовых событий. Использует `Channel` и `shareIn` для предотвращения потери событий при поворотах экрана или пересоздании View.

## Правила именования
- События экрана: `{ScreenName}Event` (например, `CatalogEvent`).
- Состояние экрана: `{ScreenName}UiState` (например, `CatalogUiState`).
- Методы навигатора: `open{Target}`, `back`.

## Организация файлов
Для каждого экрана рекомендуется следующая структура в пакете `presentation`:
- `viewmodel/`
    - `[Feature]ViewModel.kt`
    - `[Feature]UiState.kt` — только состояние экрана.
- `screen/`
    - `[Feature]Screen.kt` — точка входа (Screen + Content).
- `[Feature]Event.kt` — одноразовые события (навигация, алерты), вынесенные из `UiState` для чистоты кода и удобства использования как на UI, так и во ViewModel.

## Навигация и Back-stack
- `Catalog` является корневым экраном.
- Переход в детали (`openTile(id)`) добавляет экран в историю.
- Возврат из деталей (`back()`) или переход в каталог (`openCatalog()`) должен очищать состояние деталей, чтобы системная кнопка "Назад" на каталоге не возвращала пользователя на детали плитки.
- AR-экран считается модальным или отдельным состоянием, переход из него по `back()` возвращает на предыдущий экран (Details или Catalog).
