package com.advisely.call.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.advisely.call.CallRoute
import com.advisely.call.CallViewModel
import com.advisely.call.PermissionController
import com.advisely.call.SignalingConfig
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PermissionController.activity = this   // module 7 : l'actual Android a besoin d'une Activity
        setContent {
            // Module 9 : le ViewModel vit dans Koin et survit à la rotation, jamais dans un Composable
            val viewModel = koinViewModel<CallViewModel> { parametersOf(SignalingConfig.ROOM) }
            CallRoute(viewModel, onNavigateBack = ::finish)
        }
    }

    override fun onDestroy() {
        if (PermissionController.activity === this) PermissionController.activity = null
        super.onDestroy()
    }
}
