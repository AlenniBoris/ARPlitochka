plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

val isMacOs = System.getProperty("os.name").lowercase().contains("mac")

kotlin {
    applyDefaultHierarchyTemplate()
    if (isMacOs) {
        listOf(
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { iosTarget ->
            iosTarget.compilerOptions {
                freeCompilerArgs.add("-opt-in=kotlinx.cinterop.ExperimentalForeignApi")
            }
            iosTarget.compilations.getByName("main") {
                cinterops {
                    val planeGeometryBridge by creating {
                        defFile(project.file("src/nativeInterop/cinterop/plane_geometry_bridge.def"))
                        includeDirs.headerFilterOnly(project.file("src/nativeInterop/headers"))
                        // Fix for KLIB resolver error: cinterop doesn't need Kotlin dependencies
                        dependencyFiles = files()
                    }
                }
            }
            iosTarget.binaries.framework {
                baseName = "ARPlitkaIos"
                isStatic = true
            }
        }
    } else {
        jvm("metadataHost")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:tiles"))
            implementation(project(":shared:ui:kit"))
            implementation(project(":shared:ar:contracts"))
            implementation(project(":shared:ar:domain"))
            implementation(project(":mock:core"))
            implementation(project(":mock:tiles"))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.jetbrains.lifecycle.viewmodel)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.jetbrains.lifecycle.viewmodel.savedstate)
            implementation(libs.jetbrains.savedstate)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
        }
        
        if (isMacOs) {
            val iosMain by getting {
                dependencies {
                    // JetBrains AndroidX ports for KMP
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

if (isMacOs) {
    // Fix for KLIB resolver error in cinterop:
    // CInterop tasks don't need savedstate/lifecycle, and they cause resolution errors
    // because their KLIB unique names are often inconsistent in thin-wrapper versions.
    configurations.matching { it.name.contains("CInterop", ignoreCase = true) }.all {
        exclude(group = "org.jetbrains.androidx.savedstate")
        exclude(group = "org.jetbrains.androidx.lifecycle")
        exclude(group = "org.jetbrains.androidx.core")
    }
}
