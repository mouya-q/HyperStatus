plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.hyperstatus"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.hyperstatus"
        minSdk = 24
        targetSdk = 37
        versionCode = 6
        versionName = "0.5.1-iconify-lite"
        buildConfigField("String", "OVERLAY_ID_PREFIX", "\"HyperStatusLite_\"")
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
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }

    lint { abortOnError = false; checkReleaseBuilds = false }
}

base { archivesName = "HyperStatus-IconifyLite-v${android.defaultConfig.versionName}" }

dependencies {
    implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-preference-android:0.9.4")
    implementation("org.jetbrains.compose.ui:ui-android:1.12.0")
    implementation("org.jetbrains.compose.runtime:runtime-android:1.12.0")
    implementation("org.jetbrains.compose.foundation:foundation-android:1.12.0")
    implementation("androidx.activity:activity:1.12.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")
}
