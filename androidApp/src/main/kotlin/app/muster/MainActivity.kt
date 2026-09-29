package app.muster

import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.muster.ui.MusterApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate, or the system draws its default
        // splash instead of the themed one.
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Without this, the splash is dismissed as soon as the window has any content
        var contentDrawn = false
        val contentView: View = findViewById(android.R.id.content)
        contentView.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (!contentDrawn) return false
                    contentView.viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
            }
        )
        splashScreen.setKeepOnScreenCondition { !contentDrawn }
        splashScreen.setOnExitAnimationListener { it.remove() }

        setContent {
            MusterApp()
            SideEffect { contentDrawn = true }
        }
    }
}
