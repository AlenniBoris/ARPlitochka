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
*   `layouts`: Список раскладок (`TileLayout`). Для старых mock без `layouts` mapper строит fallback-раскладку `default` из `colors`/`variants`.
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

### `TileLayout`
Раскладка / правило повторения раппорта.
*   `id`, `name`: Идентификатор и название раскладки.
*   `previewUrl`, `defaultTextureUrl`: Превью и базовая texture для AR fallback.
*   `repeatWidthMm`, `repeatHeightMm`: Физический размер повторяемого раппорта в мм.
*   `elements`: Элементы внутри одного раппорта (`TileLayoutElement`).
*   `palettes`: Готовые палитры/миксы (`TilePalette`).

### `TileLayoutElement`
Элемент раппорта.
*   `elementTypeId`, `name`: Тип и название элемента.
*   `widthMm`, `heightMm`, `countInRepeat`: Размер и количество в одном раппорте.
*   `colorSlotId`: Цветовой слот (роль в раскладке, например `large`, `medium`).
*   `colorOptions`: Доступные цвета/SKU для слота (`TileElementColorOption`).

### `TileElementColorOption`
Цветовой вариант элемента.
*   `colorId`, `name`, `hexCode`: Цвет и отображение.
*   `textureUrl`, `swatchUrl`: Texture/swatch (опционально).
*   `sku`: SKU для расчёта заказа.

### `TilePalette`
Готовая палитра раскладки.
*   `id`, `name`: Идентификатор и название (например, «Серо-коричневый микс»).
*   `textureUrl`: **Готовая texture для AR** — быстрый render artifact повторяющегося раппорта.
*   `previewUrl`: Превью для UI.
*   `selectedColorsBySlot`: Карта `colorSlotId → colorId` для расчёта и будущего редактирования слотов.

### `TileSelection`
Выбор пользователя на экране деталей / payload для AR.
*   `tileId`, `layoutId`, `paletteId`
*   `selectedColorsBySlot`: Актуальные цвета по слотам.
*   `variantId`, `thicknessMm`: Связь с физическим вариантом (толщина, цена).

### `TileEstimateLine`
Строка локального приблизительного расчёта.
*   `sku`, `name`, `elementTypeId`, `colorId`
*   `estimatedCount`, `widthMm`, `heightMm`

---

## Раскладки vs texture preview

| Назначение | Источник | Использование |
|------------|----------|---------------|
| Быстрый AR preview | `TilePalette.textureUrl` | `BuildArTileTextureUseCase` → `ArTileTexture` |
| Расчёт заказа | `TileLayout.elements` + `countInRepeat` | `CalculateTileEstimateUseCase` |
| UI выбора микса | `TileLayout.palettes` | Экран деталей (palette selector) |

Формула локального estimate (первый этап, без упаковок и поддонов):

```text
repeatAreaM2 = repeatWidthMm * repeatHeightMm / 1_000_000
repeatCount = selectedAreaM2 / repeatAreaM2
quantity = ceil(repeatCount * countInRepeat)
```

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
      "name": "Серо-коричневый микс",
      "textureUrl": "file:///android_asset/mock/tiles/textures/kvadro_gray_mix.png",
      "hexCode": "#808080",
      "displayOrder": 0
    }
  ],
  "layouts": [
    {
      "id": "default_mix",
      "name": "Микс",
      "defaultTextureUrl": "file:///android_asset/mock/tiles/textures/kvadro_gray_mix.png",
      "repeatWidthMm": 949,
      "repeatHeightMm": 632,
      "elements": [
        {
          "elementTypeId": "kvadro_278x158",
          "name": "Квадро 278x158",
          "widthMm": 278,
          "heightMm": 158,
          "countInRepeat": 4,
          "colorSlotId": "large",
          "colorOptions": [
            { "colorId": 1, "name": "Серо-коричневый", "hexCode": "#808080", "sku": "kvadro_278x158_gray_brown" }
          ]
        }
      ],
      "palettes": [
        {
          "id": "gray_brown_mix",
          "name": "Серо-коричневый микс",
          "textureUrl": "file:///android_asset/mock/tiles/textures/kvadro_gray_mix.png",
          "selectedColorsBySlot": { "large": 1, "medium": 1, "small": 1, "mini": 1 }
        }
      ]
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

На экране деталей пользователь выбирает раскладку (если больше одной), палитру и толщину. Состояние описывается `TileSelection`: `layoutId`, `paletteId`, `selectedColorsBySlot`, `thicknessMm`/`variantId`.

*   Если у плитки одна раскладка — блок «Раскладка» скрыт.
*   Палитры отображаются в блоке «Цвета» (palette selector).
*   `onTryInAr()` передаёт `TileSelection`, а не только `tileId/colorId`.

`selectedVariant` определяется доминирующим `colorId` из палитры и выбранной толщиной.

### Какие характеристики откуда берутся

| Источник | Поля | Меняются при выборе цвета/толщины |
|----------|------|-----------------------------------|
| `TileVariant` (выбранный вариант) | цвет (отображение), высота, размеры элементов, вес м², м² в поддоне, в упаковке, в наличии, цена | да |
| `Tile` (коллекция) | класс бетона, морозостойкость, водопоглощение, класс стирания, особенности, назначение, материал, тип поверхности | нет |

Статичные характеристики вынесены на уровень `Tile`, чтобы при переключении цвета список на экране не «прыгал» из-за разных данных в вариантах.
