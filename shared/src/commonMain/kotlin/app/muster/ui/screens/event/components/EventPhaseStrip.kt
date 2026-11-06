package app.muster.ui.screens.event.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.muster.ui.common.components.MusterIcons
import app.muster.ui.screens.event.EventPhase
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.event_cancelled_strip_body
import muster.shared.generated.resources.event_cancelled_strip_title
import muster.shared.generated.resources.event_cancelled_strip_title_unknown
import muster.shared.generated.resources.event_frozen_strip
import muster.shared.generated.resources.event_locked_strip_body
import muster.shared.generated.resources.event_locked_strip_title
import muster.shared.generated.resources.event_locked_strip_title_unknown
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun EventPhaseStrip(
    phase: EventPhase,
    modifier: Modifier = Modifier
) {
    when (phase) {
        is EventPhase.Cancelled -> InkStrip(
            icon = MusterIcons.Cancelled,
            title = phase.byName
                ?.let { stringResource(Res.string.event_cancelled_strip_title, it) }
                ?: stringResource(Res.string.event_cancelled_strip_title_unknown),
            body = stringResource(Res.string.event_cancelled_strip_body),
            modifier = modifier
        )

        is EventPhase.Started -> QuietStrip(
            text = stringResource(Res.string.event_frozen_strip, phase.atDisplay),
            modifier = modifier
        )

        is EventPhase.Locked -> InkStrip(
            icon = MusterIcons.Lock,
            title = phase.byName
                ?.let { stringResource(Res.string.event_locked_strip_title, it) }
                ?: stringResource(Res.string.event_locked_strip_title_unknown),
            body = stringResource(Res.string.event_locked_strip_body),
            modifier = modifier
        )

        EventPhase.Active -> Unit
    }
}

@Composable
private fun QuietStrip(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(color = MusterColors.QuietSurface, modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MusterColors.Secondary,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun InkStrip(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Surface(color = MusterColors.Ink, contentColor = MusterColors.White, modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MusterColors.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MusterColors.White
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MusterColors.White.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Preview
@Composable
private fun EventPhaseStripStartedPreview() {
    MusterTheme {
        EventPhaseStrip(phase = EventPhase.Started(atDisplay = "7:00 pm"))
    }
}

@Preview
@Composable
private fun EventPhaseStripLockedPreview() {
    MusterTheme {
        EventPhaseStrip(phase = EventPhase.Locked(byName = "Ali"))
    }
}

@Preview
@Composable
private fun EventPhaseStripLockedUnknownPreview() {
    MusterTheme {
        EventPhaseStrip(phase = EventPhase.Locked(byName = null))
    }
}

@Preview
@Composable
private fun EventPhaseStripCancelledPreview() {
    MusterTheme {
        EventPhaseStrip(phase = EventPhase.Cancelled(byName = "Ali", started = false))
    }
}

@Preview
@Composable
private fun EventPhaseStripCancelledUnknownPreview() {
    MusterTheme {
        EventPhaseStrip(phase = EventPhase.Cancelled(byName = null, started = false))
    }
}
