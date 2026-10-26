plugins {
    alias(libs.plugins.android.application)
    // pas de kotlin("android") : AGP 9 intègre Kotlin, et ce plugin fait échouer le build
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.advisely.call.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.advisely.call"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":shared"))   // consomme la bibliothèque partagée (Compose y est déclaré en api)
    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.core)        // initKoin est appelé depuis l'application (module 7)
}
