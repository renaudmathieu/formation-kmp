package com.advisely.call

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

// Module 7 : le module qui câble l'application
val appModule = module {
    single { buildHttpClient(get()) }   // engine fourni par la plateforme
    // Module 9 : le temps du jalon, le dépôt est branché sur la fausse source (AtelierCoroutines.kt).
    // Le vrai client revient au module 11 : single { SignalingClient(get()) } bind SignalingSource::class
    single<SignalingSource> { FakeSignalingSource() }
    single { CallRepository(get()) }
    factory { CameraController() }
    factory<CameraCapture> { WebRtcCameraCapture() }   // module 7, atelier 3 : la variante interface plus DI
    factory { PermissionController() }
    viewModel { (room: String) -> CallViewModel(get(), get(), get(), room) }   // modules 9 et 10
}

// Démarrage, appelé par chaque plateforme avec son module réseau (module 6)
fun initKoin(platformModule: Module) = startKoin {
    modules(appModule, platformModule)
}
