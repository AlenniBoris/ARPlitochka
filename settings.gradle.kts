pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AR Plitka"
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
include(":shared:ui:core")
include(":shared:ui:navigation")
include(":shared:ui:kit")
include(":iosApp")
