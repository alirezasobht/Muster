package app.muster.ui.common.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.muster.ui.theme.MusterColors
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

// Delayed so a warm load, which finishes sooner, never flashes it (1q, 1s).
@Composable
fun MusterSpinner(
    delayMillis: Long,
    modifier: Modifier = Modifier,
    color: Color = MusterColors.Accent,
    trackColor: Color = color.copy(alpha = 0.35f),
    size: Dp = 26.dp,
    strokeWidth: Dp = 2.5.dp
) {
    var visible by remember { mutableStateOf(delayMillis == 0L) }
    LaunchedEffect(Unit) {
        delay(delayMillis.milliseconds)
        visible = true
    }
    if (visible) {
        CircularProgressIndicator(
            modifier = modifier.size(size),
            color = color,
            trackColor = trackColor,
            strokeWidth = strokeWidth
        )
    }
}
