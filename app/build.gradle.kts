plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    kotlin("kapt")
}

android {
    namespace = "com.example.voicefirstapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.voicefirstapp"
        minSdk = 31  // Updated to match Gallery app requirements
        targetSdk = 35  // Updated to match Gallery app
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += "-Xcontext-receivers"  // Added from Gallery app
    }
    buildFeatures {
        compose = true
        buildConfig = true  // Added from Gallery app
    }
}

dependencies {
    // Core Android dependencies (using Gallery app's libs.toml)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Navigation & Compose (using Gallery app's versions)
    implementation(libs.androidx.compose.navigation)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.material.icon.extended)

    // Work Manager & DataStore (from Gallery app)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.datastore)
    implementation(libs.com.google.code.gson)
    implementation(libs.androidx.lifecycle.process)

    // Camera dependencies (using Gallery app's versions)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // VOSK - Offline Speech Recognition
    implementation("com.alphacephei:vosk-android:0.3.47")

    // Permissions (keep your existing version since it's not in Gallery libs.toml)
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // Coroutines (keep your existing version since it's not in Gallery libs.toml)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // MediaPipe dependencies (using Gallery app's exact versions)
    implementation(libs.mediapipe.tasks.text)     // 0.10.21
    implementation(libs.mediapipe.tasks.genai)    // 0.10.25

    // TensorFlow Lite dependencies (from Gallery app)
    implementation(libs.tflite)
    implementation(libs.tflite.gpu)
    implementation(libs.tflite.support)

    // Text processing (from Gallery app)
    implementation(libs.commonmark)
    implementation(libs.richtext)

    // Protocol Buffers (from Gallery app)
    implementation(libs.protobuf.javalite)

    // Splash Screen (from Gallery app)
    implementation(libs.androidx.splashscreen)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}