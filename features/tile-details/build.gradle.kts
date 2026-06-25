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
            implementation(project(":shared:ui:core"))
            implementation(project(":shared:ui:kit"))
            implementation(project(":shared:ui:navigation"))
            implementation(project(":shared:tiles"))
            implementation(project(":mock:core"))
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
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
        }
        
        if (isMacOs) {
            val iosMain by getting {
                dependencies {
                    implementation(libs.jetbrains.savedstate)
                    implementation(libs.jetbrains.lifecycle.viewmodel)
                    implementation(libs.jetbrains.lifecycle.viewmodel.savedstate)
                    implementation(libs.jetbrains.lifecycle.viewmodel.compose)
                    implementation(libs.jetbrains.lifecycle.runtime)
                    implementation(libs.jetbrains.lifecycle.runtime.compose)
                    implementation(libs.androidx.annotation)
                    implementation(libs.jetbrains.core.bundle)
                    implementation(libs.androidx.collection)
                }
            }
        }
        
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.example.arplitka.features.tiledetails"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }
}

compose.resources {
    publicResClass = true
}
