package com.advisely.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

// Module 9 : la source de vérité de l'écran d'appel
class CallViewModel(
    private val repo: CallRepository,
    private val room: String,
) : ViewModel() {

    private val _state = MutableStateFlow(CallState())
    val state: StateFlow<CallState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<CallEffect>()   // replay à zéro : un effet ne se rejoue pas
    val effects: SharedFlow<CallEffect> = _effects.asSharedFlow()

    init {
        // le réducteur : chaque événement produit un nouvel état
        repo.events(room)
            .onEach { event -> _state.update { it.reduce(event) } }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: CallIntent) {
        when (intent) {
            CallIntent.ToggleMic -> _state.update { it.copy(micEnabled = !it.micEnabled) }
            CallIntent.HangUp -> viewModelScope.launch {
                repo.hangUp()
                _effects.emit(CallEffect.NavigateBack)
            }
        }
    }
}
