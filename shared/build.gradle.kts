plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)   // plugin Android KMP library (AGP 9)
    alias(libs.plugins.kotlin.serialization)                   // module 2
    alias(libs.plugins.skie)                                   // module 3
    alias(libs.plugins.compose.multiplatform)                  // UI partagée (module 8)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    android {
        namespace = "com.advisely.call.shared"
        compileSdk = 37   // Compose Multiplatform 1.12 tire androidx.compose 1.12, qui exige 37
        minSdk = 24
        withHostTest {}   // tests hôtes : sans eux, commonTest ne tourne pas sous Linux (module 13)
    }

    // Module 3 : le framework iOS consommé par Xcode
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"   // nom du framework vu par Xcode
            isStatic = true        // framework statique, recommandé pour démarrer
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)   // module 2
            implementation(libs.ktor.client.core)   // module 6
            implementation(libs.koin.core)           // module 7
            api(compose.runtime)                     // module 8 ; en api : androidApp en a besoin aussi
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
        androidMain.dependencies { implementation(libs.ktor.client.okhttp) }
        iosMain.dependencies { implementation(libs.ktor.client.darwin) }
    }
}
