package com.advisely.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

// Module 8 : l'écran d'appel partagé. Aucun état local : tout vient en paramètre, tout remonte en événement.
@Composable
fun CallScreen(state: CallState, onIntent: (CallIntent) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ParticipantGrid(state = state, modifier = Modifier.weight(1f))   // module 10 : la grille reçoit les pistes
        CallControls(
            micEnabled = state.micEnabled,
            onToggleMic = { onIntent(CallIntent.ToggleMic) },
            onHangUp = { onIntent(CallIntent.HangUp) }
        )
    }
}

// Module 10 : la grille affiche les pistes vidéo de l'état. Pour l'instant une seule tuile, la nôtre.
@Composable
fun ParticipantGrid(state: CallState, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        modifier = modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.localVideo?.let { track ->
            item(key = "local") { VideoRenderer(track, Modifier.aspectRatio(3f / 4f), mirror = true) }
        }
        items(state.remoteVideos.entries.toList(), key = { it.key }) { (_, track) ->
            VideoRenderer(track, Modifier.aspectRatio(3f / 4f))   // module 11
        }
        // Participants annoncés par le signaling, sans vidéo (encore) : une tuile avec leur nom
        items(state.participants.filter { it.id !in state.remoteVideos }, key = { "nom-" + it.id }) { participant ->
            Box(
                Modifier
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(participant.displayName, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun CallControls(micEnabled: Boolean, onToggleMic: () -> Unit, onHangUp: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
    ) {
        OutlinedButton(onClick = onToggleMic) {
            Text(if (micEnabled) "Couper le micro" else "Activer le micro")
        }
        Button(
            onClick = onHangUp,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
            Text("Raccrocher")
        }
    }
}
