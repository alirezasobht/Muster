package app.muster.ui.common.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme

// Shared by every empty or failed-load state that follows 1e/1t's frame:
// dashed mark, title, one line, one trailing element. Home (1e, 1t) is the
// first user; Group and Event's load failures follow the same shape until
// they get designed frames of their own.
@Composable
fun MessageState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DashedIconPlaceholder()
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MusterColors.Muted,
            textAlign = TextAlign.Center
        )
        if (trailing != null) {
            Spacer(Modifier.height(10.dp))
            trailing()
        }
    }
}

// Compose has no built-in dashed border, so it's drawn by hand.
@Composable
private fun DashedIconPlaceholder(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val stroke = Stroke(
        width = with(density) { 1.5.dp.toPx() },
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
    )
    val radius = with(density) { 16.dp.toPx() }
    Box(
        modifier = modifier.size(56.dp).drawBehind {
            drawRoundRect(
                color = MusterColors.DashedOutline,
                cornerRadius = CornerRadius(radius, radius),
                style = stroke,
                size = Size(size.width, size.height)
            )
        }
    )
}

@Preview
@Composable
private fun MessageStatePreview() {
    MusterTheme {
        Surface {
            MessageState(
                title = "No groups yet",
                body = "You haven't joined or created any groups. Ask a friend for an invite link, or start your own.",
                modifier = Modifier.padding(vertical = 32.dp)
            )
        }
    }
}

@Preview
@Composable
private fun MessageStateErrorPreview() {
    MusterTheme {
        Surface {
            MessageState(
                title = "Connection failed",
                body = "We couldn't reach the server. Please check your internet connection and try again.",
                modifier = Modifier.padding(vertical = 32.dp)
            )
        }
    }
}
