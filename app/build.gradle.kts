plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.smd_project"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.smd_project"
        minSdk = 24
        targetSdk = 36
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
    }
}

dependencies {
    // -----------------------
    // AndroidX / existing deps
    // -----------------------
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // -----------------------
    // Firebase (managed by BOM)
    // -----------------------
    // Use the BOM to keep all Firebase libraries on a consistent compatible version.
    implementation(platform("com.google.firebase:firebase-bom:32.1.0"))

    // Add the Firebase libraries WITHOUT versions — the BoM controls versions.
    implementation("com.google.firebase:firebase-auth-ktx")       // Auth
    implementation("com.google.firebase:firebase-database-ktx")   // Realtime Database
    implementation("com.google.firebase:firebase-firestore-ktx")  // Firestore

    // -----------------------
    // Coroutines support
    // -----------------------
    // Single copy of play-services coroutines helper and android coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // -----------------------
    // ROOM
    // -----------------------
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // -----------------------
    // Retrofit / Moshi / OkHttp
    // -----------------------
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    implementation("com.squareup.moshi:moshi-adapters:1.15.0")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.15.0")

    // -----------------------
    // Lifecycle
    // -----------------------
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // -----------------------
    // Security / Utils
    // -----------------------
    implementation("at.favre.lib:bcrypt:0.10.2")

    // -----------------------
    // Testing
    // -----------------------
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
