import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.teamomuito.octofiles"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.teamomuito.octofiles"
        minSdk = 30
        targetSdk = 35
        // CI sets GITHUB_RUN_NUMBER, so every build gets a higher versionCode and installs over the last.
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = "0.1.0"
    }

    // one apk per phone chip instead of one fat apk: phones are all ARM, and each apk ships only its own
    // native libs (ML Kit's were most of the size). arm64 covers almost every phone made since 2017.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = false
        }
    }

    signingConfigs {
        // the release key is committed (signing/potato-release.jks), so every release is signed
        // the same way and each one installs over the last. OCTO_KEYSTORE, if set, overrides it.
        create("release") {
            val override = System.getenv("OCTO_KEYSTORE")
            if (!override.isNullOrBlank() && file(override).exists()) {
                storeFile = file(override)
                storePassword = System.getenv("OCTO_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("OCTO_KEY_ALIAS")
                keyPassword = System.getenv("OCTO_KEY_PASSWORD")
            } else {
                storeFile = rootProject.file("signing/potato-release.jks")
                storePassword = "4b2b07015bdd485d4614c7baec4c17a5"
                keyAlias = "potato"
                keyPassword = "4b2b07015bdd485d4614c7baec4c17a5"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
        aidl = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.androidx.biometric)
    // shizuku: runs shell commands with adb's rights for the leftovers and deep cache sections. optional, no root.
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    testImplementation(libs.junit)
}
