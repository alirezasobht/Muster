package app.muster.ui.screens.event.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.muster.ui.common.util.SharedTransitionKeys
import app.muster.ui.common.util.sharedBoundsOrNone
import app.muster.ui.screens.event.EventSummary
import app.muster.ui.screens.event.EventUiState
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.event_lock_label
import muster.shared.generated.resources.event_slots_legend_in
import muster.shared.generated.resources.event_slots_legend_no_reply
import muster.shared.generated.resources.event_slots_legend_out
import muster.shared.generated.resources.event_slots_legend_pending
import muster.shared.generated.resources.event_slots_of
import muster.shared.generated.resources.event_slots_open
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.detailsSection(
    state: EventUiState.Success,
    onLockToggled: (Boolean) -> Unit
) {
    item {
        EventDetails(
            summary = state.summary,
            isFrozen = state.isStarted,
            outCount = state.outCount,
            lockSwitch = if (state.canToggleLock) {
                { LockSwitch(isLocked = state.isLocked, onToggle = onLockToggled) }
            } else {
                null
            }
        )
    }
    item { Spacer(Modifier.height(16.dp)) }
}

@Composable
internal fun EventDetails(
    summary: EventSummary,
    isFrozen: Boolean,
    // Null: breakdown not loaded yet (Loading/Error) — panel shows the bar with
    // no legend, same as a genuinely empty event.
    outCount: Int? = null,
    lockSwitch: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.date,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.sharedBoundsOrNone(SharedTransitionKeys.eventDate(summary.eventId))
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = summary.location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusterColors.Secondary,
                    modifier = Modifier.sharedBoundsOrNone(SharedTransitionKeys.eventLocation(summary.eventId))
                )
            }
            if (lockSwitch != null) {
                Spacer(Modifier.width(16.dp))
                lockSwitch()
            }
        }
        Spacer(Modifier.height(16.dp))
        SlotsPanel(
            capacity = summary.capacity,
            inCount = summary.inCount,
            pendingCount = summary.pendingCount,
            outCount = outCount,
            isFrozen = isFrozen,
            modifier = Modifier.sharedBoundsOrNone(SharedTransitionKeys.eventStats(summary.eventId))
        )
    }
}

@Composable
private fun SlotsPanel(
    capacity: Int,
    inCount: Int,
    pendingCount: Int,
    outCount: Int?,
    isFrozen: Boolean,
    modifier: Modifier = Modifier
) {
    val open = (capacity - inCount - pendingCount).coerceAtLeast(0)
    val hasLegend = outCount != null && (inCount + pendingCount + outCount) > 0

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MusterColors.QuietSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = (inCount + pendingCount).toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MusterColors.Ink
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(Res.string.event_slots_of, capacity),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusterColors.Muted,
                    modifier = Modifier.weight(1f).padding(bottom = 3.dp)
                )
                Text(
                    text = stringResource(Res.string.event_slots_open, open),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MusterColors.Accent,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            SlotsBar(open = open, inCount = inCount, pendingCount = pendingCount)
            if (hasLegend) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LegendItem(color = MusterColors.Accent, label = stringResource(Res.string.event_slots_legend_in, inCount))
                    Spacer(Modifier.width(12.dp))
                    LegendItem(
                        color = MusterColors.PendingText,
                        label = if (isFrozen) {
                            stringResource(Res.string.event_slots_legend_no_reply, pendingCount)
                        } else {
                            stringResource(Res.string.event_slots_legend_pending, pendingCount)
                        }
                    )
                    Spacer(Modifier.width(12.dp))
                    LegendItem(color = MusterColors.OutText, label = stringResource(Res.string.event_slots_legend_out, outCount))
                }
            }
        }
    }
}

@Composable
private fun SlotsBar(
    open: Int,
    inCount: Int,
    pendingCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(percent = 50)).background(MusterColors.Hairline)
    ) {
        if (inCount > 0) Box(Modifier.weight(inCount.toFloat()).fillMaxHeight().background(MusterColors.Accent))
        if (pendingCount > 0) Box(Modifier.weight(pendingCount.toFloat()).fillMaxHeight().background(MusterColors.PendingText))
        if (open > 0) Spacer(Modifier.weight(open.toFloat()))
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MusterColors.Muted)
    }
}

// The whole column toggles, so the label is the switch's accessible name.
@Composable
internal fun LockSwitch(
    isLocked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.toggleable(value = isLocked, role = Role.Switch, onValueChange = onToggle),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.event_lock_label),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MusterColors.Secondary
        )
        Spacer(Modifier.height(4.dp))
        Switch(
            checked = isLocked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                uncheckedThumbColor = MusterColors.Hint,
                uncheckedTrackColor = MusterColors.White,
                uncheckedBorderColor = MusterColors.Hint
            )
        )
    }
}

@Preview
@Composable
private fun EventDetailsPreview() {
    MusterTheme {
        Surface {
            EventDetails(summary = PreviewSummary, isFrozen = false, outCount = 1, modifier = Modifier.padding(16.dp))
        }
    }
}

@Preview
@Composable
private fun EventDetailsFrozenPreview() {
    MusterTheme {
        Surface {
            EventDetails(summary = PreviewSummary, isFrozen = true, outCount = 1, modifier = Modifier.padding(16.dp))
        }
    }
}

@Preview
@Composable
private fun EventDetailsWithLockSwitchPreview() {
    MusterTheme {
        Surface {
            EventDetails(
                summary = PreviewSummary,
                isFrozen = false,
                outCount = 1,
                lockSwitch = { LockSwitch(isLocked = false, onToggle = {}) },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
