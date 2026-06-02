plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

val isMacOs = System.getProperty("os.name").lowercase().contains("mac")

kotlin {
    if (isMacOs) {
        listOf(
            iosX64(),
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { iosTarget ->
            iosTarget.compilations.getByName("main") {
                cinterops {
                    val planeGeometryBridge by creating {
                        defFile(project.file("src/nativeInterop/cinterop/plane_geometry_bridge.def"))
                        includeDirs.headerFilterOnly(project.file("src/nativeInterop/headers"))
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
            implementation(project(":shared:app"))
            implementation(project(":shared:ar:contracts"))
            implementation(project(":shared:ar:domain"))
            implementation(project(":shared:ui:kit"))
            implementation(project(":mock:core"))
            implementation(project(":mock:tiles"))
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
