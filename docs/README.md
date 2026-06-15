# Документация проекта

В этой папке есть два типа документов:

- **Канонические документы** — актуальные продуктовые и инженерные контракты. Их нужно читать в первую очередь.
- **Исторические документы** — планы задач, расследования, handoff-prompts и отладочные заметки. Они полезны как контекст, но не являются источником истины, если уже есть канонический документ.

## Канонические Документы

| Область | Документ |
|---------|----------|
| Архитектура | [ARCHITECTURE.md](./ARCHITECTURE.md) |
| Модульность | [MODULARIZATION.md](./MODULARIZATION.md) |
| Навигация | [NAVIGATION.md](./NAVIGATION.md) |
| Тестирование | [TESTING.md](./TESTING.md) |
| Обзор AR | [ar/README.md](./ar/README.md) |
| Поиск поверхностей в AR | [ar/PLATFORM_SURFACE_DETECTION.md](./ar/PLATFORM_SURFACE_DETECTION.md) |
| Стабильность контура в AR | [ar/PLATFORM_CONTOUR_STABILITY.md](./ar/PLATFORM_CONTOUR_STABILITY.md) |
| Заливка плиткой в AR | [ar/PLATFORM_TILE_FILLING.md](./ar/PLATFORM_TILE_FILLING.md) |
| Паритет AR-платформ | [ar/PLATFORM_PARITY.md](./ar/PLATFORM_PARITY.md) |
| Архитектурные решения | [decisions/](./decisions/) |
| Backend и mock-слой | [BACKEND_MOCKING_PLAN.md](./BACKEND_MOCKING_PLAN.md) |

## Исторические Документы И Расследования

Эти документы сохраняют контекст задач и анализов. Для текущего поведения предпочитайте канонические документы выше:

- [archive/completed-plans/SURFACE_DETECTION.md](./archive/completed-plans/SURFACE_DETECTION.md) — старый объединённый AR-reference; канонические части разнесены в `docs/ar/`.
- [archive/completed-plans/IOS_AR_SURFACE_STRATEGY.md](./archive/completed-plans/IOS_AR_SURFACE_STRATEGY.md)
- [archive/completed-plans/IOS_AR_CONTINUOUS_FLOOR_PLACEMENT_PLAN.md](./archive/completed-plans/IOS_AR_CONTINUOUS_FLOOR_PLACEMENT_PLAN.md)
- [archive/completed-plans/ios-ar-point-stability.md](./archive/completed-plans/ios-ar-point-stability.md)
- [archive/completed-plans/ios-tile-placement.md](./archive/completed-plans/ios-tile-placement.md)
- [investigations/ios-ar-point-stability-task.md](./investigations/ios-ar-point-stability-task.md)
- [investigations/ios-ar-delegate-starvation-handoff-prompt.md](./investigations/ios-ar-delegate-starvation-handoff-prompt.md)
- [investigations/tile-fill-parity-analysis.md](./investigations/tile-fill-parity-analysis.md)
- [investigations/android-contour-visibility.md](./investigations/android-contour-visibility.md)
