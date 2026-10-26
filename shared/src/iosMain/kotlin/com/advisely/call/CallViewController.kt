package com.advisely.call

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

// Module 8 : iosMain, pas commonMain (UIViewController n'existe que sur iOS). Exposé à Swift comme un UIViewController.
fun CallViewController(viewModel: CallViewModel, onNavigateBack: () -> Unit = {}): UIViewController =
    ComposeUIViewController {
        CallRoute(viewModel, onNavigateBack)
    }
