plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
    id("kotlin-parcelize")
    id("com.google.gms.google-services")

}

android {
    namespace = "com.mobitechs.parcelwala"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mobitechs.parcelwala"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BASE_URL", "\"https://api.yourapp.com/v1/\"")

        // Load API key from local.properties
        val properties = org.jetbrains.kotlin.konan.properties.Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
        }

        val mapsApiKey = properties.getProperty("MAPS_API_KEY", "")

        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey

        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")



    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Networking - Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Image Loading - Coil
    implementation("io.coil-kt:coil-compose:2.5.0")



    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Splash Screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // Material Icons Extended
    implementation("androidx.compose.material:material-icons-extended:1.6.0")

    // Google Maps & Places
    implementation("com.google.maps.android:maps-compose:4.3.3")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.libraries.places:places:3.5.0")
    implementation("com.google.android.gms:play-services-location:21.1.0")

    // For polyline utilities (decode polyline)
    implementation("com.google.maps.android:android-maps-utils:3.8.2")


    // Firebase BOM (manages versions)
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))

    // Firebase Cloud Messaging
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Optional: Analytics
    implementation("com.google.firebase:firebase-analytics-ktx")


    // Add SignalR dependency
    implementation("com.microsoft.signalr:signalr:7.0.0")
    implementation("io.reactivex.rxjava3:rxjava:3.1.8")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // Debug
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Razorpay Payment Gateway
    implementation("com.razorpay:checkout:1.6.41")

}

kapt {
    correctErrorTypes = true
}