package com.advisely.call

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

// Module 7 : le module qui câble l'application
val appModule = module {
    single { buildHttpClient(get()) }   // engine fourni par la plateforme
    // Module 11 : le vrai client revient, à la place de la fausse source du module 9
    single { SignalingClient(get()) } bind SignalingSource::class   // module 6, interface du module 4
    single { CallRepository(get()) }
    factory { CameraController() }
    factory<CameraCapture> { WebRtcCameraCapture() }   // module 7, atelier 3 : la variante interface plus DI
    factory { PermissionController() }
    viewModel { (room: String) -> CallViewModel(get(), get(), get(), get(), room) }   // modules 9, 10 et 11
}

// Démarrage, appelé par chaque plateforme avec son module réseau (module 6)
fun initKoin(platformModule: Module) = startKoin {
    modules(appModule, platformModule)
}
