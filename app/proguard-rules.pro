# ARCore ProGuard Rules
-keep class com.google.ar.core.** { *; }
-keep class com.google.ar.sceneform.** { *; }
-keep class io.github.sceneview.** { *; }

# Filament (used by SceneView)
-keep class com.google.android.filament.** { *; }

# DI (Koin)
-keep class org.koin.** { *; }
-dontwarn org.koin.**
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep domain models
-keep class com.example.arplitka.features.floordetection.domain.model.** { *; }

# Keep SceneView internal JNI calls
-keepclasseswithmembernames class * {
    native <methods>;
}
