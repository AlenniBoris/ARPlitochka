pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    }
}

rootProject.name = "ArPlitka"
include(":app")
include(":features:floor-detection")
include(":features:catalog")
include(":network:core")
include(":mock:core")
include(":mock:tiles")
include(":shared:ar:core")
include(":shared:ar:contracts")
include(":shared:ar:domain")
include(":shared:app")
include(":shared:tiles")
include(":shared:core")
include(":shared:validation")
include(":shared:ui:core")
include(":shared:ui:navigation")
include(":shared:ui:kit")
include(":iosApp")
