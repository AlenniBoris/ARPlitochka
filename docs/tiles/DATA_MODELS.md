# Модели данных и API (Tiles)

В проекте используется иерархическая модель данных для плитки, которая позволяет гибко описывать коллекции, цвета и физические характеристики.

## Доменные модели (`Domain Layer`)

### `Tile`
Основная сущность плитки (коллекция).
*   `id`: Уникальный идентификатор.
*   `name`: Название коллекции.
*   `description`: Описание.
*   `manufacturer`: Производитель (строка).
*   `category`: Категория (например, "paving_stones").
*   `unit`: Единица измерения (`M2`, `PIECE`, `BOX`).
*   `basePrice`: Базовая цена для отображения в каталоге.
*   `photos`: Список URL-адресов фотографий.
*   `colors`: Список доступных цветовых решений (`TileColor`).
*   `variants`: Список физических размеров и характеристик (`TileVariant`).
*   `websiteUrl`: Ссылка на товар на сайте (опционально).
*   `usageWays`: Список назначений (`TileUsageWay`).
*   `features`: Список особенностей (`TileFeature`).
*   `concreteClass`: Класс бетона (общий для коллекции).
*   `frostResistance`: Морозостойкость (общий для коллекции).
*   `waterAbsorptionPercent`: Водопоглощение (общий для коллекции).
*   `abrasionClass`: Класс стойкости к стиранию (общий для коллекции).

### `TileColor`
Цветовое решение плитки.
*   `id`: ID цвета.
*   `name`: Название (например, "Серый").
*   `textureUrl`: URL текстуры для наложения в AR.
*   `hexCode`: Код цвета для отображения в UI.
*   `swatchUrl`: URL превью для цветового кружка в UI (fallback: `textureUrl`).
*   `displayOrder`: Порядок отображения в списке цветов.

### `TileVariant`
Конкретный физический вариант плитки.
*   `id`: ID варианта.
*   `colorId`: Ссылка на `TileColor`.
*   `widthMm`, `heightMm`, `thicknessMm`: Размеры в миллиметрах.
*   `price`: Цена за конкретный вариант.
*   `stockCount`: Остаток на складе.
*   `tilesPerBox`: Количество штук в упаковке (опционально).
*   `elementSizes`: Список размеров элементов в паттерне (`TileElementSize`).
*   `weightKgPerM2`: Вес м² в кг.
*   `m2PerPallet`: Количество м² в поддоне.

### `TileElementSize`
Размер одного элемента в составе варианта/паттерна.
*   `widthMm`, `heightMm`: Размеры элемента.
*   `label`: Опциональное название элемента.
*   `quantityInPattern`: Опциональное количество в паттерне.

### `TileUsageWay` (enum)
*   `HOME_AND_GARDEN` — Дом и сад
*   `PUBLIC_SPACE` — Общественное пространство
*   `DRIVEWAY` — Подъездные пути
*   `PEDESTRIAN_AREA` — Пешеходные зоны

### `TileFeature` (enum)
*   `MICRO_BEVEL` — Микрофаска
*   `BEVEL` — Фаска
*   `ANTI_SLIP` — Противоскользящая поверхность
*   `FROST_RESISTANT` — Морозостойкая
*   `COLOR_MIX` — Смешанный цвет
*   `TEXTURED_SURFACE` — Фактурная поверхность

---

## Пример JSON (`tile_1.json`)

Файл находится в `:mock:tiles/src/main/assets/mock/tiles/details/tile_1.json`.

```json
{
  "id": 1,
  "name": "Квадро",
  "websiteUrl": "https://example.com/tiles/quadro",
  "usageWays": ["HOME_AND_GARDEN", "PUBLIC_SPACE"],
  "features": ["MICRO_BEVEL", "FROST_RESISTANT"],
  "concreteClass": "B30",
  "frostResistance": "F200",
  "waterAbsorptionPercent": "≤ 6%",
  "abrasionClass": "3 (H)",
  "colors": [
    {
      "id": 1,
      "name": "Серый",
      "textureUrl": "file:///android_asset/mock/tiles/textures/paving_stones_v1.png",
      "hexCode": "#808080",
      "swatchUrl": "file:///android_asset/mock/tiles/textures/paving_stones_v1.png",
      "displayOrder": 0
    }
  ],
  "variants": [
    {
      "id": 101,
      "colorId": 1,
      "widthMm": 278,
      "heightMm": 158,
      "thicknessMm": 60,
      "price": 690.0,
      "stockCount": 150,
      "elementSizes": [
        { "widthMm": 278, "heightMm": 158 },
        { "widthMm": 265, "heightMm": 158 }
      ],
      "weightKgPerM2": 144.0,
      "m2PerPallet": 10.70
    }
  ]
}
```

Технические поля `concreteClass`, `frostResistance`, `waterAbsorptionPercent`, `abrasionClass` задаются на уровне `Tile` (корень JSON). В `variants` остаются только параметры, которые зависят от цвета/толщины.

Для обратной совместимости `TileDtoMapper` поднимает эти четыре поля из первого варианта, если в корне JSON они не указаны (старые mock-файлы).

## Маппинг данных
1.  **`TileDto`**: Отражает структуру JSON. Использует `@Serializable`.
2.  **`TileDtoMapper`**: Преобразует DTO в доменную модель. На этом этапе происходит валидация данных и установка значений по умолчанию.
3.  **`ExceptionMapper`**: Преобразует технические ошибки (`Throwable`) в доменные исключения (`TileException`).

## Экран деталей

На экране деталей пользователь выбирает цвет и толщину. `selectedVariant` определяется парой `colorId + thicknessMm`. По умолчанию выбираются первый цвет и первая доступная толщина для этого цвета.

### Какие характеристики откуда берутся

| Источник | Поля | Меняются при выборе цвета/толщины |
|----------|------|-----------------------------------|
| `TileVariant` (выбранный вариант) | цвет (отображение), высота, размеры элементов, вес м², м² в поддоне, в упаковке, в наличии, цена | да |
| `Tile` (коллекция) | класс бетона, морозостойкость, водопоглощение, класс стирания, особенности, назначение, материал, тип поверхности | нет |

Статичные характеристики вынесены на уровень `Tile`, чтобы при переключении цвета список на экране не «прыгал» из-за разных данных в вариантах.
