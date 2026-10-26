package com.advisely.call

import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

// Point d'entrée pour Swift : initKoin y serait renommé doInitKoin (le préfixe init est réservé)
fun startKoinIos() {
    initKoin(iosNetworkModule)
    // Module 7, atelier 6 : vérifier la résolution au démarrage
    println("Koin prêt : ${KoinPlatform.getKoin().get<CallRepository>()}")
}

// Module 9 : le ViewModel pour Swift. Hors Compose, il n'appartient à aucun ViewModelStore :
// ici il vit aussi longtemps que l'application, qui n'a qu'un écran.
fun callViewModel(room: String): CallViewModel = KoinPlatform.getKoin().get { parametersOf(room) }
