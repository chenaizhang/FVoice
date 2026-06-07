plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
}

android {
    namespace = "com.fvoice.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.fvoice.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = System.getenv("VERSION_NAME") ?: "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("release.keystore")
            storePassword = System.getenv("STORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    defaultConfig {
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17 -O3 -fno-finite-math-only"
                arguments += "-DANDROID_STL=c++_shared"
            }
        }
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    sourceSets {
        getByName("androidTest") {
            assets.setSrcDirs(listOf("src/main/cpp/whisper.cpp/samples"))
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigationevent.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.miuix.navigation3.ui)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.miuix.ui)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.blur)
    implementation(libs.materialkolor)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.opencc4j)

    // ONNX Runtime for SileroVAD
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.26.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
