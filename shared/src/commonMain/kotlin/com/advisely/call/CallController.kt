package com.advisely.call

import kotlinx.coroutines.delay

/**
 * Bouchon fourni par le socle (module 3). Au module 11, ce rôle est repris par CallEngine.
 *
 * createOffer renvoie un String et non une SessionDescription : ce type arrive avec webrtc-kmp au module 7,
 * un homonyme maison créerait des imports ambigus au module 11.
 */
class CallController {
    suspend fun createOffer(): String {
        delay(300)   // simule la négociation
        return "v=0\r\no=- 0 0 IN IP4 127.0.0.1\r\ns=SDP factice du socle\r\nt=0 0\r\n"
    }
}
