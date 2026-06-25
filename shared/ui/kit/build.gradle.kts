plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

val isMacOs = System.getProperty("os.name").lowercase().contains("mac")

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    
    if (isMacOs) {
        listOf(
            iosArm64(),
            iosSimulatorArm64()
        )
    } else {
        jvm("metadataHost")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:ui:core"))
            implementation(project(":shared:tiles"))
            implementation(project(":mock:core"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.coil.compose)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
        }
    }
}

android {
    namespace = "com.example.arplitka.shared.ui.kit"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

compose.resources {
    publicResClass = true
}
