package com.advisely.call

// Module 7 : le cas expect/actual incontournable. Deux mécanismes très différents derrière une seule fonction.
expect class PermissionController() {
    suspend fun ensureCameraAndMic(): Boolean
}
