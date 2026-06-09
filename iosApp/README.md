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
./gradlew :iosApp:linkDebugFrameworkIosSimulatorArm64
./gradlew :shared:app:iosSimulatorArm64Test
./gradlew :shared:ar:contracts:iosSimulatorArm64Test
```

Then create/open an Xcode iOS app target that imports the generated `ARPlitkaIos` framework and uses the Swift files from `iosApp/iosApp`.
