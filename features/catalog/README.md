# Module `:features:catalog`

Feature-модуль для отображения каталога плитки и выбора материалов для AR-примерки.

## Назначение

Модуль отвечает за:
- отображение списка доступной плитки в виде сетки (Staggered Grid);
- поддержку Pull to Refresh для обновления данных;
- отображение состояний загрузки и ошибок с возможностью повтора;
- форматирование цен и единиц измерения плитки;
- выбор конкретной модели плитки для перехода в AR.

## Структура

```text
features/catalog/
├── build.gradle.kts
└── src/commonMain/kotlin/com/example/arplitka/features/catalog/
    ├── presentation/
    │   ├── screen/
    │   │   └── CatalogScreen.kt (основной экран со списком и Pull to Refresh)
    │   └── viewmodel/
    │       ├── CatalogViewModel.kt (логика загрузки и обновления)
    │       └── CatalogUiState.kt (Loading, Content, Error)
    └── domain/
        └── usecase/
```

## Навигация

Экран каталога является стартовым экраном приложения и доступен через Bottom Navigation Bar.

## Особенности реализации

- **Сетка**: Используется `LazyVerticalStaggeredGrid` для отображения плитки в две колонки.
- **Pull to Refresh**: Интегрирован `PullToRefreshBox` с кастомным индикатором `AppRefreshIndicator`.
- **Локализация**: Цены и единицы измерения (`м²`, `ед.`, `уп.`) берутся из строковых ресурсов `:shared:ui:core`.
- **Изображения**: Используется `TilePreviewImage` (expect/actual) для оптимальной загрузки превью.
