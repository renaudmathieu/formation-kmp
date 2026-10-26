// Fichier de départ de l'atelier du module 4. À copier dans shared/src/commonMain/kotlin/com/advisely/call/
// une fois SignalingSource déclarée (code du module 4, section 3) et SignalMessage annotée (module 2).
// La branche etape/05-structure contient la version complétée.
package com.advisely.call

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.io.IOException

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

/** Atelier, étape 5 : un faux flux qui échoue une collecte sur deux, pour tester retryWhen. */
class FlakySignalingSource(private val delegate: SignalingSource = FakeSignalingSource()) : SignalingSource {
    private var attempts = 0
    override fun incoming(room: String): Flow<SignalMessage> = flow {
        attempts++
        if (attempts % 2 == 1) throw IOException("Connexion coupée (simulée), tentative $attempts")
        emitAll(delegate.incoming(room))
    }
}

// Atelier, étapes 1 et 2 : à compléter.
// val callScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
// fun leaveCall() { callScope.cancel() }
fun demoScope() {
    val callScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    callScope.launch { /* TODO : une boucle avec println et delay */ }
    callScope.launch { /* TODO : une seconde boucle, puis lever une exception (étape 2) */ }
    // TODO : annuler le scope et vérifier que les deux s'arrêtent
}
