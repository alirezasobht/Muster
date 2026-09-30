package app.muster

import androidx.compose.ui.window.ComposeUIViewController
import app.muster.ui.MusterApp
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    initKoin()
    return ComposeUIViewController { MusterApp() }
}
