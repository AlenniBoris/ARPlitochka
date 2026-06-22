plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

val isMacOs = System.getProperty("os.name").lowercase().contains("mac")

kotlin {
    applyDefaultHierarchyTemplate()
    androidTarget()
    if (isMacOs) {
        listOf(
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { iosTarget ->
            iosTarget.compilerOptions {
                freeCompilerArgs.add("-opt-in=kotlinx.cinterop.ExperimentalForeignApi")
            }
        }
    } else {
        jvm("metadataHost")
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core"))
            implementation(project(":network:core"))
            implementation(project(":shared:ar:contracts"))
            implementation(project(":shared:tiles"))
            implementation(project(":features:catalog"))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.jetbrains.lifecycle.viewmodel)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.jetbrains.lifecycle.viewmodel.savedstate)
            implementation(libs.jetbrains.savedstate)
            
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
        }
        
        if (isMacOs) {
            val iosMain by getting {
                dependencies {
                    // JetBrains AndroidX ports for KMP
                    api(libs.jetbrains.savedstate)
                    api(libs.jetbrains.lifecycle.viewmodel)
                    api(libs.jetbrains.lifecycle.viewmodel.savedstate)
                    api(libs.jetbrains.lifecycle.viewmodel.compose)
                    api(libs.jetbrains.lifecycle.runtime)
                    api(libs.jetbrains.lifecycle.runtime.compose)
                    api(libs.androidx.annotation)
                    api(libs.jetbrains.core.bundle)
                    api(libs.androidx.collection)
                }
            }
        }
        
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.example.arplitka.shared.app"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }
}
