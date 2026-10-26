package com.advisely.call

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * L'écran d'appel branché sur le CallViewModel (module 9), commun à Android et iOS :
 * l'état en StateFlow, les effets ponctuels consommés une seule fois.
 */
@Composable
fun CallRoute(viewModel: CallViewModel, onNavigateBack: () -> Unit = {}) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.joinCall()   // module 11 : caméra puis appel, à l'entrée dans l'écran (idempotent)
        viewModel.effects.collect { effect ->
            when (effect) {
                is CallEffect.ShowError -> snackbar.showSnackbar(effect.message)
                CallEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    MaterialTheme {
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                CallScreen(state, viewModel::onIntent)
            }
        }
    }
}
