package app.muster.ui.screens.event.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.muster.domain.error.DomainError
import app.muster.domain.model.RsvpStatus
import app.muster.ui.common.components.FormError
import app.muster.ui.common.components.PrimaryButton
import app.muster.ui.common.toMessage
import app.muster.ui.screens.event.EventPhase
import app.muster.ui.screens.event.EventUiState
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.action_cant_make_it
import muster.shared.generated.resources.action_change
import muster.shared.generated.resources.action_im_in
import muster.shared.generated.resources.event_rsvp_frozen_in
import muster.shared.generated.resources.event_rsvp_frozen_out
import muster.shared.generated.resources.event_rsvp_frozen_pending
import muster.shared.generated.resources.event_rsvp_in
import muster.shared.generated.resources.event_rsvp_locked
import muster.shared.generated.resources.event_rsvp_out
import muster.shared.generated.resources.event_rsvp_question
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.ownRsvpSection(
    state: EventUiState.Success,
    onRsvp: (RsvpStatus) -> Unit
) {
    if (state.myStatus == null) return
    item { OwnRsvpBlock(state = state, onRsvp = onRsvp) }
    item { Spacer(Modifier.height(20.dp)) }
}

@Composable
internal fun OwnRsvpBlock(
    state: EventUiState.Success,
    onRsvp: (RsvpStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val status = state.myStatus ?: return
    val started = state.isStarted

    if (state.isReadOnly) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MusterColors.QuietSurface
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        when {
                            !started -> Res.string.event_rsvp_locked
                            status == RsvpStatus.In -> Res.string.event_rsvp_frozen_in
                            status == RsvpStatus.Out -> Res.string.event_rsvp_frozen_out
                            else -> Res.string.event_rsvp_frozen_pending
                        }
                    ),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f)
                )
                RsvpStatusBadge(status = status, isFrozen = started)
            }
        }
        return
    }

    // Local, ephemeral: reopening the two buttons is a display toggle, not a
    // write. Keyed on myStatus so a successful change collapses it back.
    var isChanging by remember(state.myStatus) { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MusterColors.Outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (status == RsvpStatus.Pending || isChanging) {
                Text(
                    text = stringResource(Res.string.event_rsvp_question),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(Modifier.height(12.dp))
                // A label that wraps on a narrow screen grows both buttons together.
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    PrimaryButton(
                        text = stringResource(Res.string.action_im_in),
                        onClick = {
                            isChanging = false
                            onRsvp(RsvpStatus.In)
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(
                        onClick = {
                            isChanging = false
                            onRsvp(RsvpStatus.Out)
                        },
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MusterColors.Outline),
                        modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 52.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.action_cant_make_it),
                            style = MaterialTheme.typography.labelLarge,
                            color = MusterColors.Ink,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(
                            if (status == RsvpStatus.In) Res.string.event_rsvp_in else Res.string.event_rsvp_out
                        ),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = { isChanging = true },
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MusterColors.Outline)
                    ) {
                        Text(text = stringResource(Res.string.action_change), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            if (state.rsvpError != null) {
                Spacer(Modifier.height(12.dp))
                FormError(message = state.rsvpError.toMessage())
            }
        }
    }
}

@Preview
@Composable
private fun OwnRsvpBlockUnansweredPreview() {
    MusterTheme {
        Surface {
            OwnRsvpBlock(state = PreviewSuccess.copy(myStatus = RsvpStatus.Pending), onRsvp = {}, modifier = Modifier.padding(16.dp))
        }
    }
}

@Preview
@Composable
private fun OwnRsvpBlockInPreview() {
    MusterTheme {
        Surface {
            OwnRsvpBlock(state = PreviewSuccess.copy(myStatus = RsvpStatus.In), onRsvp = {}, modifier = Modifier.padding(16.dp))
        }
    }
}

@Preview
@Composable
private fun OwnRsvpBlockOutPreview() {
    MusterTheme {
        Surface {
            OwnRsvpBlock(state = PreviewSuccess.copy(myStatus = RsvpStatus.Out), onRsvp = {}, modifier = Modifier.padding(16.dp))
        }
    }
}

@Preview
@Composable
private fun OwnRsvpBlockErrorPreview() {
    MusterTheme {
        Surface {
            OwnRsvpBlock(state = PreviewSuccess.copy(myStatus = RsvpStatus.Pending, rsvpError = DomainError.EventFull()), onRsvp = {}, modifier = Modifier.padding(16.dp))
        }
    }
}

@Preview
@Composable
private fun OwnRsvpBlockLockedPreview() {
    MusterTheme {
        Surface {
            OwnRsvpBlock(state = PreviewSuccess.copy(myStatus = RsvpStatus.In, phase = EventPhase.Locked(byName = "Ali")), onRsvp = {}, modifier = Modifier.padding(16.dp))
        }
    }
}

@Preview
@Composable
private fun OwnRsvpBlockStartedPreview() {
    MusterTheme {
        Surface {
            OwnRsvpBlock(state = PreviewSuccess.copy(myStatus = RsvpStatus.In, phase = EventPhase.Started(atDisplay = "7:00 pm")), onRsvp = {}, modifier = Modifier.padding(16.dp))
        }
    }
}
