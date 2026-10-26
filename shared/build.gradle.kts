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
        compileSdk = 36
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

    // Module 7 : supprime l'avertissement Beta des classes expect/actual
    compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)   // module 2
            implementation(libs.ktor.client.core)   // module 6
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.koin.core)           // module 7
            implementation(libs.webrtc.kmp)          // module 7 : MediaStream, VideoTrack, PeerConnection
            api(compose.runtime)                     // module 8 ; en api : androidApp en a besoin aussi
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.ktor.client.mock)    // module 6
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.activity)   // module 7 : PermissionController
        }
        iosMain.dependencies { implementation(libs.ktor.client.darwin) }
    }
}
