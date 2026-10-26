package com.advisely.call

import org.koin.mp.KoinPlatform

// Point d'entrée pour Swift : initKoin y serait renommé doInitKoin (le préfixe init est réservé)
fun startKoinIos() {
    initKoin(iosNetworkModule)
    // Atelier 6 : vérifier la résolution au démarrage
    println("Koin prêt : ${KoinPlatform.getKoin().get<CallRepository>()}")
}
