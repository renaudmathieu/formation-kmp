package com.advisely.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shepeliev.webrtckmp.MediaStream
import com.shepeliev.webrtckmp.audioTracks   // extensions de MediaStream : à importer
import com.shepeliev.webrtckmp.videoTracks
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Module 9 : la source de vérité de l'écran d'appel. Module 10 : le média local. Module 11 : la connexion.
class CallViewModel(
    private val repo: CallRepository,
    private val signaling: SignalingClient,          // module 11
    private val permissions: PermissionController,   // module 10
    private val camera: CameraController,            // module 10
    private val room: String,
) : ViewModel() {

    private val _state = MutableStateFlow(CallState())
    val state: StateFlow<CallState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<CallEffect>()   // replay à zéro : un effet ne se rejoue pas
    val effects: SharedFlow<CallEffect> = _effects.asSharedFlow()

    private var localStream: MediaStream? = null   // gardé pour le module 11
    private var cameraJob: Job? = null

    // Module 11 : le bloc init du module 9, qui collectait repo.events(room) sur le faux signaling, est supprimé.
    // Avec le vrai client, il ouvrirait une seconde socket (et échouerait avant join()) : c'est le moteur qui
    // collecte le signaling, une seule fois, et nourrit le réducteur.
    // Module 12 : le moteur maillé remplace CallEngine. Une PeerConnection par pair, à deux comme à trois.
    private val engine = MeshCallEngine(signaling, viewModelScope, _state)   // après la déclaration de _state
    private var callJob: Job? = null

    // Module 10. Idempotent : une rotation d'écran rejoue l'appel, mais pas la capture
    fun startCamera(): Job = cameraJob ?: viewModelScope.launch {
        val stream = runCatching { startLocalMedia(permissions, camera) }.getOrElse { e ->
            // pas de caméra (simulateur iOS), caméra occupée… : un message plutôt qu'un plantage
            _effects.emit(CallEffect.ShowError("Caméra indisponible : ${e.message}"))
            return@launch
        }
        if (stream == null) {
            _effects.emit(CallEffect.ShowError("Caméra ou micro refusé"))   // sans lui : écran noir muet
            return@launch
        }
        localStream = stream
        _state.update { it.copy(localVideo = stream.videoTracks.first()) }
    }.also { cameraJob = it }

    // À appeler à l'entrée de l'écran : attend la caméra, puis rejoint l'appel.
    // Idempotent : une rotation d'écran ne rouvre ni caméra ni socket.
    fun joinCall() {
        if (callJob != null) return
        callJob = viewModelScope.launch {
            startCamera().join()                           // module 10 : attend la réponse à la permission
            val stream = localStream ?: return@launch      // refusé : ShowError a déjà été émis
            runCatching { engine.start(room, stream) }.onFailure { e ->
                _state.update { it.copy(status = Status.Error) }   // serveur injoignable : un message, pas un plantage
                _effects.emit(CallEffect.ShowError("Connexion au signaling impossible : ${e.message}"))
            }
        }
    }

    private fun hangUp() {
        engine.stop()
        camera.stop()
    }

    override fun onCleared() = hangUp()   // l'écran est détruit (Activity finie) : on libère tout

    fun onIntent(intent: CallIntent) {
        when (intent) {
            // le micro affiché doit être le micro réel
            CallIntent.ToggleMic -> {
                val enabled = !_state.value.micEnabled
                localStream?.audioTracks?.forEach { it.enabled = enabled }
                _state.update { it.copy(micEnabled = enabled) }
            }
            CallIntent.HangUp -> viewModelScope.launch {
                hangUp()                       // module 11 : à la place de repo.hangUp()
                _effects.emit(CallEffect.NavigateBack)
            }
        }
    }
}
