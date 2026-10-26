package com.advisely.call.android

import android.app.Application
import com.advisely.call.CallRepository
import com.advisely.call.androidNetworkModule
import com.advisely.call.initKoin

// Module 7 : Koin démarre une seule fois par processus (pas dans l'Activity, recréée à chaque rotation)
class CallApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val koin = initKoin(androidNetworkModule).koin
        // Atelier 6 : vérifier la résolution au démarrage (retirer androidNetworkModule pour voir l'erreur Koin)
        println("Koin prêt : ${koin.get<CallRepository>()}")
    }
}
