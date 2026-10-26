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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Module 9 : la source de vérité de l'écran d'appel. Module 10 : le média local.
class CallViewModel(
    private val repo: CallRepository,
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

    init {
        // le réducteur : chaque événement produit un nouvel état
        repo.events(room)
            .onEach { event -> _state.update { it.reduce(event) } }
            .launchIn(viewModelScope)
    }

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

    fun onIntent(intent: CallIntent) {
        when (intent) {
            // le micro affiché doit être le micro réel
            CallIntent.ToggleMic -> {
                val enabled = !_state.value.micEnabled
                localStream?.audioTracks?.forEach { it.enabled = enabled }
                _state.update { it.copy(micEnabled = enabled) }
            }
            CallIntent.HangUp -> viewModelScope.launch {
                camera.stop()                  // libère caméra et micro
                repo.hangUp()
                _effects.emit(CallEffect.NavigateBack)
            }
        }
    }
}
