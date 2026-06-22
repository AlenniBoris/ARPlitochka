# iosApp

iOS application shell for AR Plitka.

The Kotlin module builds a static framework named `ARPlitkaIos` on macOS and exposes:

```kotlin
fun MainViewController(): UIViewController
```

The Swift files in `iosApp/iosApp` show the expected SwiftUI entry point that hosts the Compose Multiplatform root.

On Windows this module uses a JVM metadata host target so Gradle can configure and validate common code without requiring Xcode.

## iOS AR documentation

- [Point stability & anchor corrections](../docs/ios-ar-point-stability.md)
- [Placement mode](../docs/IOS_AR_CONTINUOUS_FLOOR_PLACEMENT_PLAN.md)
- [Surface strategy](../docs/IOS_AR_SURFACE_STRATEGY.md)

## Mac verification

Run on macOS with Xcode installed:

```shell
./gradlew :iosApp:linkDebugFrameworkIosArm64
```

For the iOS Simulator:

```shell
./gradlew :iosApp:linkDebugFrameworkIosSimulatorArm64
```

Then open `iosApp/xcode/ARPlitkaIos/ARPlitkaIos.xcodeproj` in Xcode and Run.

The Xcode target embeds the framework from `iosApp/build/bin/iosArm64/debugFramework/` and copies Compose Multiplatform resources from `ComposeBundleResources/` into the app bundle (`compose-resources/composeResources/...`). This fixes missing assets such as `ic_loading_tile.png` from `:shared:ui:kit` without running Gradle inside Xcode.

Additional checks:

```shell
./gradlew :shared:app:iosSimulatorArm64Test
./gradlew :shared:ar:contracts:iosSimulatorArm64Test
```
