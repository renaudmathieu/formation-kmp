package com.advisely.call

import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.core.context.startKoin

// Module 7 : le module qui câble l'application
val appModule = module {
    single { buildHttpClient(get()) }   // engine fourni par la plateforme
    single { SignalingClient(get()) } bind SignalingSource::class   // module 6, interface du module 4
    single { CallRepository(get()) }
    factory { CameraController() }
    factory<CameraCapture> { WebRtcCameraCapture() }   // atelier 3 : la variante interface plus DI
    factory { PermissionController() }
}

// Démarrage, appelé par chaque plateforme avec son module réseau (module 6)
fun initKoin(platformModule: Module) = startKoin {
    modules(appModule, platformModule)
}
