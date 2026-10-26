package com.advisely.call.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.advisely.call.PermissionController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PermissionController.activity = this   // module 7 : l'actual Android a besoin d'une Activity
        setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Câblage prêt. L'écran d'appel arrive au module 8.")
                }
            }
        }
    }

    override fun onDestroy() {
        if (PermissionController.activity === this) PermissionController.activity = null
        super.onDestroy()
    }
}
