# iosApp

Оболочка iOS-приложения для AR Plitka.

Kotlin-модуль собирает статический фреймворк с именем `ARPlitkaIos` на macOS и предоставляет:

```kotlin
fun MainViewController(): UIViewController
```

Swift-файлы в `iosApp/iosApp` представляют собой точку входа SwiftUI, которая хостит корень Compose Multiplatform.

На Windows этот модуль использует целевой хост метаданных JVM, чтобы Gradle мог настраивать и проверять общий код без необходимости использования Xcode.

## Документация по iOS AR

- [Стабильность точек и коррекция анкоров](../docs/ios-ar-point-stability.md)
- [Режим размещения](../docs/IOS_AR_CONTINUOUS_FLOOR_PLACEMENT_PLAN.md)
- [Стратегия поверхностей](../docs/IOS_AR_SURFACE_STRATEGY.md)

## Процесс разработки на Mac

### Предварительные требования
- macOS с установленным Xcode.
- Установленная Android Studio (предоставляет необходимый JDK).

### Сборка и запуск
Мы используем официальную интеграцию Compose Multiplatform. Вам не нужно собирать фреймворк вручную из терминала.

1. Откройте `iosApp/xcode/ARPlitkaIos/ARPlitkaIos.xcodeproj` в Xcode.
2. Убедитесь, что путь к JDK указан правильно в фазе сборки **Compile Kotlin Framework** (по умолчанию используется JBR из Android Studio).
3. Выберите цель (симулятор или устройство).
4. Нажмите **Run** (Cmd + R).

Xcode автоматически:
- Запустит Gradle для сборки фреймворка через `:iosApp:embedAndSignAppleFrameworkForXcode`.
- Упакует ресурсы Compose Multiplatform (файлы `.cvr`, изображения) в бандл приложения.
- Подпишет и запустит приложение.

### Решение проблем
- **Отсутствующие ресурсы или старый код**: Выполните **Product -> Clean Build Folder** (Cmd + Shift + K) в Xcode и запустите снова.
- **Ошибки Java**: Проверьте путь `JAVA_HOME` в фазе сборки Xcode "Compile Kotlin Framework". Он должен указывать на валидный JDK (например, `/Applications/Android Studio.app/Contents/jbr/Contents/Home`).

### Ручные проверки (опционально)
```shell
./gradlew :shared:app:iosSimulatorArm64Test
./gradlew :shared:ar:contracts:iosSimulatorArm64Test
```
