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

### `TileColor`
Цветовое решение плитки.
*   `id`: ID цвета.
*   `name`: Название (например, "Серый").
*   `textureUrl`: URL текстуры для наложения в AR.
*   `hexCode`: Код цвета для отображения в UI.

### `TileVariant`
Конкретный физический вариант плитки.
*   `id`: ID варианта.
*   `colorId`: Ссылка на `TileColor`.
*   `widthMm`, `heightMm`, `thicknessMm`: Размеры в миллиметрах.
*   `price`: Цена за конкретный вариант.
*   `stockCount`: Остаток на складе.
*   `tilesPerBox`: Количество штук в упаковке (опционально).

---

## Пример JSON (`all.json`)

Файл находится в `:mock:tiles/src/main/assets/mock/tiles/catalog/all.json`.

```json
{
  "items": [
    {
      "id": 1,
      "name": "Classic Paving Stone",
      "manufacturer": "Plitka Pro",
      "unit": "m2",
      "basePrice": 95.0,
      "colors": [
        {
          "id": 1,
          "name": "Серый",
          "textureUrl": "file:///android_asset/mock/tiles/textures/paving_stones_v1.png",
          "hexCode": "#808080"
        }
      ],
      "variants": [
        {
          "id": 101,
          "colorId": 1,
          "widthMm": 260,
          "heightMm": 260,
          "price": 95.0,
          "stockCount": 150
        }
      ]
    }
  ]
}
```

## Маппинг данных
1.  **`TileDto`**: Отражает структуру JSON. Использует `@Serializable`.
2.  **`TileDtoMapper`**: Преобразует DTO в доменную модель. На этом этапе происходит валидация данных и установка значений по умолчанию.
3.  **`ExceptionMapper`**: Преобразует технические ошибки (`Throwable`) в доменные исключения (`TileException`).
