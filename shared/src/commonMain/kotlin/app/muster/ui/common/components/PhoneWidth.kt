package app.muster.ui.common.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// The screens are designed at 390 dp. The cap is a little wider so a phone
// in landscape and a small tablet gain some room without the layout losing
// its shape; past that it stays centred and the caller's background fills
// the rest of the window.
@Composable
fun PhoneWidth(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(
            modifier = modifier
                .widthIn(max = MAX_CONTENT_WIDTH)
                .fillMaxWidth()
                .fillMaxHeight(),
            content = content
        )
    }
}

private val MAX_CONTENT_WIDTH = 480.dp
