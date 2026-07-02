pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.android.application") {
                useModule("com.android.tools.build:gradle:${requested.version}")
            }
        }
    }
    repositories {
        maven { url = uri("../.gradle/android-plugin-cache") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("../.gradle/android-plugin-cache") }
        google()
        mavenCentral()
    }
}

rootProject.name = "YouTubeTextLensTranslatorAndroid"
include(":app")
