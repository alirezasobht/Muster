package app.muster.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.HtmlElementView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlinx.browser.document
import org.w3c.dom.HTMLIFrameElement

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun WebPage(url: String, modifier: Modifier) {
    // The <iframe> sits above the canvas and can't join a screen transition, so
    // it would float over the next screen. Shown only while its screen is RESUMED.
    val lifecycleState = LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    HtmlElementView(
        factory = {
            (document.createElement("iframe") as HTMLIFrameElement).apply {
                src = url
                style.setProperty("width", "100%")
                style.setProperty("height", "100%")
                style.setProperty("border", "none")
            }
        },
        modifier = modifier,
        update = { iframe ->
            val onScreen = lifecycleState.value.isAtLeast(Lifecycle.State.RESUMED)
            iframe.style.setProperty("visibility", if (onScreen) "visible" else "hidden")
        }
    )
}
