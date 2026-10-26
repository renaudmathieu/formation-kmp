// Atelier du module 4, version complétée (fichier de départ : ateliers/AtelierCoroutines.kt).
package com.advisely.call

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.io.IOException   // en code commun : pas de java.io

/**
 * Faux signaling : des participants arrivent et repartent, chacun avec un `from` distinct.
 * Le flux tourne en boucle, pour que l'écran du module 9 bouge en direct.
 * Il sert aux modules 4 et 9 ; le vrai SignalingClient le remplace au module 11.
 */
class FakeSignalingSource(private val stepMillis: Long = 1_500) : SignalingSource {
    override fun incoming(room: String): Flow<SignalMessage> = flow {
        while (true) {
            emit(SignalMessage(type = "join", from = "alice"))
            delay(stepMillis)
            emit(SignalMessage(type = "join", from = "bob"))
            delay(stepMillis)
            emit(SignalMessage(type = "join", from = "chloe"))
            delay(stepMillis)
            emit(SignalMessage(type = "leave", from = "bob"))
            delay(stepMillis)
            emit(SignalMessage(type = "leave", from = "alice"))
            delay(stepMillis)
            emit(SignalMessage(type = "leave", from = "chloe"))
            delay(stepMillis)
        }
    }
}

/** Étape 5 : un faux flux qui échoue une collecte sur deux. */
class FlakySignalingSource(private val delegate: SignalingSource = FakeSignalingSource()) : SignalingSource {
    private var attempts = 0
    override fun incoming(room: String): Flow<SignalMessage> = flow {
        attempts++
        if (attempts % 2 == 1) throw IOException("Connexion coupée (simulée), tentative $attempts")
        emitAll(delegate.incoming(room))
    }
}

// Étapes 1 et 2 : un scope par appel. Annuler le scope arrête tout ce qu'il contient.
val callScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

fun startDemoCoroutines() {
    callScope.launch { repeat(10) { println("collecte A $it"); delay(200) } }
    callScope.launch { repeat(10) { println("collecte B $it"); delay(300) } }
    // Étape 2 : avec Job() au lieu de SupervisorJob(), une exception ici tuerait aussi la collecte A.
}

fun leaveCall() { callScope.cancel() }   // tout s'arrête proprement

// Étape 5 : reconnexion avec backoff croissant, bornée à trois tentatives
fun SignalingSource.incomingWithRetry(room: String): Flow<SignalMessage> =
    incoming(room).retryWhen { cause, attempt ->
        if (cause is IOException && attempt < 3) {
            delay(1000L * (attempt + 1))   // backoff croissant
            true
        } else false
    }
