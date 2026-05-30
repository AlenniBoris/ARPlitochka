plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
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
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.example.arplitka.shared.ar.contracts"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }
}
