# ADR-002: Repeat Pattern Не Обязан Совпадать С Размером Плитки

## Статус

Принято.

## Контекст

Текстуры плитки в каталоге содержат metadata `repeatPattern`:

- `widthMm`
- `lengthMm`

Tile variants тоже содержат физические размеры, например `widthMm` и `lengthMm`. Это разные понятия.

## Решение

`repeatPattern` задаёт физический размер одного полного повторяющегося изображения-текстуры на AR-полу. Это не обязательно размер одного отдельного камня или tile variant.

Для текущих текстур paving stones:

- `repeatPattern.widthMm = 780`
- `repeatPattern.lengthMm = 1040`

Это соответствует renderer constants, которые сейчас используются Android и iOS:

- `0.78 m`
- `1.04 m`

## Последствия

- Pixel size изображения управляет качеством/плотностью bitmap, а не физическим размером на полу.
- Renderer должен использовать `repeatPattern`, чтобы определить частоту повторения текстуры в метрах.
- Hardcoded renderer constants допустимы только как временный compatibility bridge.
- В долгосрочной схеме AR renderers должны получать metadata выбранной текстуры из shared/catalog state.

## Связанные Документы

- `docs/ar/PLATFORM_TILE_FILLING.md`
- `docs/BACKEND_MOCKING_PLAN.md`
