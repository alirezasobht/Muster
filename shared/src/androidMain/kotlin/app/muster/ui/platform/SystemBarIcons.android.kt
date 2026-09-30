package app.muster.ui.platform

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
actual fun SystemBarIcons(dark: Boolean) {
    val window = LocalView.current.context.findActivity()?.window ?: return
    SideEffect {
        SystemBarIconColor.dark = dark
        SystemBarIconColor.apply(window)
    }
}

// Removing the splash view resets the icons to white, so MainActivity
// re-applies the last value the screen asked for.
object SystemBarIconColor {
    internal var dark = true

    fun apply(window: Window) {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = dark
            isAppearanceLightNavigationBars = dark
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
