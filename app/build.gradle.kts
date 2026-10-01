plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.hyperstatus"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.hyperstatus"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0-miuix"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-preference-android:0.9.4")
    implementation("org.jetbrains.compose.ui:ui-android:1.12.0")
    implementation("org.jetbrains.compose.runtime:runtime-android:1.12.0")
    implementation("org.jetbrains.compose.foundation:foundation-android:1.12.0")
    implementation("androidx.activity:activity:1.12.0")
    implementation("com.android.tools.build:apksig:8.7.3")
}