plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
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
            implementation(project(":shared:ar:contracts"))
            implementation(project(":shared:tiles"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
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
