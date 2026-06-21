plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

import java.util.Properties

android {
    namespace = "com.textlens.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.textlens.android"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
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

            val keystorePath = signingProperties.getProperty("TEXTLENS_KEYSTORE_PATH")
                ?: System.getenv("TEXTLENS_KEYSTORE_PATH")
            val keystorePassword = signingProperties.getProperty("TEXTLENS_KEYSTORE_PASSWORD")
                ?: System.getenv("TEXTLENS_KEYSTORE_PASSWORD")
            val keyAlias = signingProperties.getProperty("TEXTLENS_KEY_ALIAS")
                ?: System.getenv("TEXTLENS_KEY_ALIAS")
            val keyPassword = signingProperties.getProperty("TEXTLENS_KEY_PASSWORD")
                ?: System.getenv("TEXTLENS_KEY_PASSWORD")

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

dependencies {
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.foundation:foundation-android:1.7.6")
    implementation("androidx.compose.material3:material3-android:1.3.1")
    implementation("androidx.compose.ui:ui-android:1.7.6")
    implementation("androidx.compose.ui:ui-tooling-preview-android:1.7.6")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    debugImplementation("androidx.compose.ui:ui-tooling-android:1.7.6")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.6")
}
