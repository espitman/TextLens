plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

import java.util.Properties

android {
    namespace = "com.textlens.tv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.textlens.tv"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
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

            val keystorePath = signingProperties.getProperty("TEXTLENS_TV_KEYSTORE_PATH")
                ?: System.getenv("TEXTLENS_TV_KEYSTORE_PATH")
            val keystorePassword = signingProperties.getProperty("TEXTLENS_TV_KEYSTORE_PASSWORD")
                ?: System.getenv("TEXTLENS_TV_KEYSTORE_PASSWORD")
            val keyAlias = signingProperties.getProperty("TEXTLENS_TV_KEY_ALIAS")
                ?: System.getenv("TEXTLENS_TV_KEY_ALIAS")
            val keyPassword = signingProperties.getProperty("TEXTLENS_TV_KEY_PASSWORD")
                ?: System.getenv("TEXTLENS_TV_KEY_PASSWORD")

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
    implementation("io.ktor:ktor-server-core-jvm:2.3.12")
    implementation("io.ktor:ktor-server-cio-jvm:2.3.12")
}
