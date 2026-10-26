package com.advisely.call

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shepeliev.webrtckmp.VideoTrack

// Module 10 : le rendu vidéo passe par une vue native par plateforme, pontée dans Compose
@Composable
expect fun VideoRenderer(
    track: VideoTrack,
    modifier: Modifier = Modifier,
    mirror: Boolean = false,   // true pour la vue locale, false pour les vues distantes (module 11)
)
