package app.muster.ui.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.muster.ui.theme.MusterColors

// The screens are designed at 390 dp. The cap is a little wider so a phone
// in landscape and a small tablet gain some room without the layout losing
// its shape; past that it stays centred and the surround takes the rest.
//
// The safe-drawing inset lives here so no screen has to remember it, and
// so it covers content anchored to the edges — a FAB aligned to the bottom
// of a Box would otherwise sit under the three-button nav bar. Compose
// consumes insets it applies, so a screen that also calls
// safeDrawingPadding() is a no-op rather than double padding.
@Composable
fun PhoneWidth(
    modifier: Modifier = Modifier,
    // Transparent for screens that own a background colour — 1a, 1q and 1r
    // stay green edge to edge. The surround exists to give a white screen an
    // edge, and green already has one.
    surround: Color = MusterColors.Hairline,
    content: @Composable BoxScope.() -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        // Below this the column fills the window and there is no surround to
        // paint. Between the cap and here the sliver is too thin to read as
        // anything but a seam.
        val wide = maxWidth >= WIDE_VIEWPORT
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (wide) surround else Color.Transparent)
        )
        Box(
            modifier = modifier
                .widthIn(max = MAX_CONTENT_WIDTH)
                .fillMaxWidth()
                .fillMaxHeight()
                .background(if (wide && surround != Color.Transparent) MusterColors.White else Color.Transparent)
                .safeDrawingPadding(),
            content = content
        )
    }
}

private val MAX_CONTENT_WIDTH = 480.dp
private val WIDE_VIEWPORT = 560.dp
