# iOS: размещение плитки (tile placement parity)

**Статус:** реализовано в коде (июнь 2026), требует проверки на iPhone  
**Ветка:** `feature/ios_ar_screen`

Android-референс: `ArActionButtons.kt`, `FloorArViewModel.kt`, `ArSceneLayer.kt`.

Связанные документы:
- [SURFACE_DETECTION.md](./SURFACE_DETECTION.md) — общий статус Android/iOS, чеклисты регрессии
- [android-contour-visibility.md](./android-contour-visibility.md) — контракт visibility точек/линий/fill
- [TESTING.md](./TESTING.md) — shared domain tests (`FloorArControllerTileTest`)

---

## Что реализовано

| Область | Файлы |
|---------|-------|
| Shared state/events | `TileModels.kt`, `FloorContourUiState.kt`, `FloorArContracts.kt`, `FloorArController.kt` |
| Domain tests | `FloorArControllerTileTest.kt`, `FloorContourUiPublishTest.kt` |
| iOS UI | `IosArScreen.ios.kt` — «Добавить/Убрать/Сменить плитку», панель поворота |
| SceneKit fill | `IosArContourRenderer.kt` — синяя заливка / текстура с кэшем материалов |
| Текстуры | `pg_load_tile_texture_image` в `plane_geometry_bridge.def` |

### Shared контракт

- `TileType`: `paving_stones_v1`, `paving_stones_v2` (по умолчанию v2)
- `TextureRotation`: 0° → 45° → 90° → 135° → 0°
- События: `ToggleTileVisibility`, `ChangeTileType`, `RotateTexture`
- В режиме плитки: `showContourPoints == false`, `showContourLines == false`, `showSectionFill == true`

### iOS ресурсы

- `iosApp/iosApp/Resources/mock/tiles/textures/paving_stones_v1.png`
- `iosApp/iosApp/Resources/mock/tiles/textures/paving_stones_v2.png`
- Копии в `iosApp/xcode/ARPlitkaIos/Resources/mock/tiles/textures/`

---

## Сборка (Windows)

```bat
.\gradlew.bat :shared:ar:domain:testDebugUnitTest
.\gradlew.bat :iosApp:compileKotlinMetadata
.\gradlew.bat :app:compileDebugKotlin
```

## Сборка (Mac / устройство)

```bash
./gradlew :iosApp:linkDebugFrameworkIosArm64
```

Открыть Xcode-проект `iosApp/xcode/ARPlitkaIos.xcodeproj`, собрать на iPhone.

---

## Чеклист QA на iPhone

1. **Финализация зоны** — поставить ≥3 точки, замкнуть контур, нажать «Готово».
2. **Добавить плитку** — появляется кнопка «Добавить плитку»; после нажатия полигон заполняется текстурой (не синим).
3. **Сменить плитку** — переключение v1 ↔ v2 (`Сменить плитку`).
4. **Поворот** — «Повернуть» циклично 0° / 45° / 90° / 135°; угол отображается в панели.
5. **Убрать плитку** — возвращается синяя заливка, точки и линии контура снова видны.
6. **Выровнять контур** — кнопка остаётся доступной над блоком управления; после выравнивания плитка/контур не ломаются.
7. **Пересканировать** — сбрасывает контур, плитку и текстуру; начинается новый скан.

### Debug panel (debug build)

- `Tile`: On/Off
- `Texture rotation`: 0 / 45 / 90 / 135
- `Tile type`: `paving_stones_v1` / `paving_stones_v2`

---

## Известные ограничения

- Текстура генерируется в native bridge по bounds полигона (как Android `toSectionPatternBitmap`), с UV 0–1 на `pg_create_contour_fill_geometry`.
- Если PNG не попал в app bundle, заливка может остаться белой — проверить `Resources/mock/tiles/textures/*.png` в Xcode target.
