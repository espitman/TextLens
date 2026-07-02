plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

import java.util.Properties

android {
    namespace = "com.textlens.youtubetranslatorandroid"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.textlens.youtubetranslatorandroid"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.1.2"
    }

    buildFeatures {
        compose = true
    }

    signingConfigs {
        getByName("debug") {
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = false
            enableV4Signing = false
        }
        create("release") {
            val signingProperties = Properties()
            val signingPropertiesFile = rootProject.file("local.properties")
            if (signingPropertiesFile.exists()) {
                signingPropertiesFile.inputStream().use(signingProperties::load)
            }

            val keystorePath = signingProperties.getProperty("YTLENS_TRANSLATOR_KEYSTORE_PATH")
                ?: System.getenv("YTLENS_TRANSLATOR_KEYSTORE_PATH")
            val keystorePassword = signingProperties.getProperty("YTLENS_TRANSLATOR_KEYSTORE_PASSWORD")
                ?: System.getenv("YTLENS_TRANSLATOR_KEYSTORE_PASSWORD")
            val keyAlias = signingProperties.getProperty("YTLENS_TRANSLATOR_KEY_ALIAS")
                ?: System.getenv("YTLENS_TRANSLATOR_KEY_ALIAS")
            val keyPassword = signingProperties.getProperty("YTLENS_TRANSLATOR_KEY_PASSWORD")
                ?: System.getenv("YTLENS_TRANSLATOR_KEY_PASSWORD")

            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = false
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    lint {
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("androidx.compose.material:material-ripple"))
            .using(module("androidx.compose.material:material-ripple-android:1.7.5"))
        substitute(module("androidx.lifecycle:lifecycle-runtime-compose"))
            .using(module("androidx.lifecycle:lifecycle-runtime-compose-android:2.8.3"))
    }
}

dependencies {
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.foundation:foundation-android:1.7.5")
    implementation("androidx.compose.material3:material3-android:1.3.1")
    implementation("androidx.compose.ui:ui-android:1.7.5")
    implementation("androidx.compose.ui:ui-tooling-preview-android:1.7.5")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling-android:1.7.6")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.6")
}
