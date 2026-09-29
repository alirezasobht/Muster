package app.muster.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Shows a web page inside the screen: WebView on Android, WKWebView on iOS,
// an <iframe> on web.
@Composable
expect fun WebPage(
    url: String,
    modifier: Modifier = Modifier
)
