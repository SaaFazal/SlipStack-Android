plugins {
    alias(libs.plugins.android.application)
    // Add this so Firebase can read google-services.json
    id("com.google.gms.google-services")
}

android {
    namespace = "com.n1249874.slipstack"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.n1249874.slipstack"
        minSdk = 24
        targetSdk = 34
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

    // Use ViewBinding everywhere
    buildFeatures { viewBinding = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    // Fix for 16KB page size alignment (required for ML Kit on newer Android versions)
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    // UI basics
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Navigation (fragments + UI)
    implementation("androidx.navigation:navigation-fragment:2.8.4")
    implementation("androidx.navigation:navigation-ui:2.8.4")

    // CameraX 1.4.0 — 16KB page-size aligned ✅
    implementation("androidx.camera:camera-core:1.4.0")
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")

    // ML Kit Text Recognition (GMS bundled) — 16KB aligned ✅
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")

    // Guava — required by CameraX for ListenableFuture
    implementation("com.google.guava:guava:32.1.3-android")

    // Firebase (use BoM to pin versions)
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    // Note: Using local storage for images instead of Firebase Storage (cost optimization)

    // Preferences & background work (for weekly reminder / retries)
    implementation("androidx.preference:preference:1.2.1")
    implementation("androidx.work:work-runtime:2.9.1")

    // Room database
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // LiveData + ViewModel
    implementation("androidx.lifecycle:lifecycle-livedata:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
