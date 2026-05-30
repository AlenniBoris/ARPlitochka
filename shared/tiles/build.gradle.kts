plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

val isMacOs = System.getProperty("os.name").lowercase().contains("mac")

kotlin {
    androidTarget()
    if (isMacOs) {
        iosX64()
        iosArm64()
        iosSimulatorArm64()
    } else {
        jvm("metadataHost")
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":network:core"))
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.example.arplitka.shared.tiles"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }
}
