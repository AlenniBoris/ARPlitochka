plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.arplitka"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.arplitka"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":features:floor-detection"))
    implementation(project(":features:catalog"))
    implementation(project(":shared:ui:navigation"))
    implementation(project(":shared:ui:core"))
    implementation(project(":shared:ui:kit"))
    implementation(project(":shared:app"))
    implementation(project(":shared:core"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore)
    implementation(project(":network:core"))
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.arcore)
    implementation(libs.sceneview.ar)

    debugImplementation(libs.androidx.ui.tooling)
    implementation(project(":mock:core"))
    implementation(project(":mock:tiles"))
}
