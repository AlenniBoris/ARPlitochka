# Module `:app`

Application-модуль Android-приложения AR Plitka.

## Назначение

`app` собирает финальное Android-приложение и отвечает за инфраструктуру верхнего уровня:

- application id и Android manifest;
- точка входа `MainActivity`;
- Hilt application class;
- проверка runtime-разрешения `CAMERA`;
- проверка поддержки ARCore;
- подключение основного feature-модуля `:features:floor-detection`.

В этом модуле не должна находиться бизнес-логика определения пола, разметки контура или будущего взаимодействия с backend. Он должен оставаться тонким слоем сборки и запуска приложения.

## Основные Файлы

```text
app/
├── build.gradle.kts
├── proguard-rules.pro
└── src/main/
    ├── AndroidManifest.xml
    └── java/com/example/arplitka/
        ├── ArPlitkaApplication.kt
        └── MainActivity.kt
```

## Ключевые Классы

### `ArPlitkaApplication`

Application class с аннотацией `@HiltAndroidApp`. Запускает Hilt dependency graph для всего приложения.

### `MainActivity`

Главная и единственная Activity приложения.

Отвечает за:

- edge-to-edge режим через `WindowCompat.setDecorFitsSystemWindows(window, false)`;
- Compose entry point;
- Material theme;
- `CameraPermissionGate`;
- `ArCoreAvailabilityGate`;
- запуск `FloorArScreen`.

## Manifest

Модуль объявляет:

- разрешение `android.permission.CAMERA`;
- обязательную обычную камеру;
- обязательную AR-камеру `android.hardware.camera.ar`;
- ARCore metadata `com.google.ar.core = required`;
- portrait orientation для `MainActivity`.

## Зависимости Модуля

```kotlin
implementation(project(":features:floor-detection"))
implementation(project(":shared:ar"))
implementation(project(":shared:ui"))
```

Основные внешние зависимости:

- Jetpack Compose;
- Material 3;
- Hilt;
- ARCore;
- SceneView AR.

## Build Configuration

Текущие параметры:

- `compileSdk = 36`;
- `minSdk = 26`;
- `targetSdk = 36`;
- Java/Kotlin target `17`;
- release-сборка включает `isMinifyEnabled = true`;
- release-сборка включает `isShrinkResources = true`.

## Границы Ответственности

В `:app` допустимо добавлять:

- навигацию верхнего уровня, когда появятся новые экраны;
- application-wide DI;
- theme setup;
- permission gates;
- app-level error screens.

В `:app` не стоит добавлять:

- расчет геометрии;
- работу с ARCore frame/plane/hit-test;
- сетевые запросы конкретной фичи;
- логику выбора и хранения точек;
- backend DTO конкретной фичи.
