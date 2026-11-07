package app.muster.ui.screens.event.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.muster.domain.error.DomainError
import app.muster.domain.model.RsvpStatus
import app.muster.ui.common.components.InitialsAvatar
import app.muster.ui.common.components.MessageState
import app.muster.ui.common.components.PrimaryButton
import app.muster.ui.common.toMessage
import app.muster.ui.screens.event.EventUiState
import app.muster.ui.screens.event.RosterAction
import app.muster.ui.screens.event.RosterRow
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.event_action_cancel_invite
import muster.shared.generated.resources.event_action_remove_from_event
import muster.shared.generated.resources.event_action_remove_from_list
import muster.shared.generated.resources.event_action_resend_invite
import muster.shared.generated.resources.event_add
import muster.shared.generated.resources.event_add_players
import muster.shared.generated.resources.event_players_header
import muster.shared.generated.resources.event_roster_empty_body
import muster.shared.generated.resources.event_roster_empty_title
import muster.shared.generated.resources.event_status_no_reply
import muster.shared.generated.resources.events_status_in
import muster.shared.generated.resources.events_status_out
import muster.shared.generated.resources.events_status_pending
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.rosterSection(
    state: EventUiState.Success,
    showAddAction: Boolean,
    isFrozen: Boolean,
    onAddPlayers: () -> Unit,
    onRowAction: (row: RosterRow, action: RosterAction) -> Unit
) {
    item {
        RosterHeader(
            playerCount = state.roster.size,
            showAddPill = showAddAction && !state.isRosterEmpty,
            onAddPlayers = onAddPlayers
        )
    }

    if (state.isRosterEmpty) {
        item { RosterEmptyState(showAddAction = showAddAction, onAddPlayers = onAddPlayers) }
    } else {
        rosterRows(state = state, isFrozen = isFrozen, onRowAction = onRowAction)
    }
}

@Composable
private fun RosterHeader(
    playerCount: Int,
    showAddPill: Boolean,
    onAddPlayers: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(Res.string.event_players_header, playerCount),
            style = MaterialTheme.typography.titleMedium,
            color = MusterColors.Ink,
            modifier = Modifier.weight(1f)
        )
        if (showAddPill) {
            AddPill(text = stringResource(Res.string.event_add), onClick = onAddPlayers)
        }
    }
}

@Composable
private fun RosterEmptyState(
    showAddAction: Boolean,
    onAddPlayers: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        MessageState(
            title = stringResource(Res.string.event_roster_empty_title),
            body = stringResource(Res.string.event_roster_empty_body),
            trailing = if (showAddAction) {
                {
                    PrimaryButton(
                        text = stringResource(Res.string.event_add_players),
                        onClick = onAddPlayers,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                null
            }
        )
    }
}

private fun LazyListScope.rosterRows(
    state: EventUiState.Success,
    isFrozen: Boolean,
    onRowAction: (row: RosterRow, action: RosterAction) -> Unit
) {
    itemsIndexed(state.roster, key = { _, row -> row.id }) { index, row ->
        RosterRowItem(
            row = row,
            isFrozen = isFrozen,
            readOnly = state.isReadOnly,
            inFlight = state.rowActionTargetId == row.id,
            errorMessage = state.rowActionError?.toMessage()
                ?.takeIf { state.rowActionErrorId == row.id },
            showDivider = index < state.roster.lastIndex,
            onAction = { action -> onRowAction(row, action) },
            modifier = Modifier.animateItem()
        )
    }
}

@Composable
private fun RosterRowItem(
    row: RosterRow,
    isFrozen: Boolean,
    readOnly: Boolean,
    inFlight: Boolean,
    errorMessage: String?,
    showDivider: Boolean,
    onAction: (RosterAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InitialsAvatar(row.initials)
            Spacer(Modifier.width(12.dp))
            Text(
                text = row.name + if (row.isSelf) " · you" else "",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(8.dp))
            StatusMenu(
                status = row.status,
                inFlight = inFlight,
                rowActions = row.actions,
                isFrozen = isFrozen,
                readOnly = readOnly,
                onAction = onAction
            )
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MusterColors.OutText,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        if (showDivider) HorizontalDivider(color = MusterColors.Hairline)
    }
}

@Composable
private fun StatusMenu(
    status: RsvpStatus,
    inFlight: Boolean,
    rowActions: List<RosterAction>,
    isFrozen: Boolean,
    readOnly: Boolean,
    onAction: (RosterAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasAction = rowActions.isNotEmpty() && !readOnly
    var expanded by remember(hasAction) { mutableStateOf(false) }
    Box(modifier = modifier) {
        RsvpStatusBadge(
            status = status,
            isFrozen = isFrozen,
            chevron = hasAction,
            onClick = if (hasAction) ({ expanded = true }) else null
        )
        if (hasAction) {
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                rowActions.forEach { action ->
                    DropdownMenuItem(
                        text = { Text(text = stringResource(action.label())) },
                        onClick = {
                            expanded = false
                            onAction(action)
                        },
                        enabled = !inFlight
                    )
                }
            }
        }
    }
}

private fun RosterAction.label() = when (this) {
    RosterAction.SetIn -> Res.string.events_status_in
    RosterAction.SetOut -> Res.string.events_status_out
    RosterAction.CancelInvite -> Res.string.event_action_cancel_invite
    RosterAction.RemoveFromEvent -> Res.string.event_action_remove_from_event
    RosterAction.RemoveFromList -> Res.string.event_action_remove_from_list
    RosterAction.ResendInvite -> Res.string.event_action_resend_invite
}

@Composable
internal fun RsvpStatusBadge(
    status: RsvpStatus,
    isFrozen: Boolean,
    chevron: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val noReply = isFrozen && status == RsvpStatus.Pending
    val fill = when {
        noReply -> MusterColors.White
        status == RsvpStatus.In -> MusterColors.InFill
        status == RsvpStatus.Pending -> MusterColors.PendingFill
        else -> MusterColors.OutFill
    }
    val text = when {
        noReply -> MusterColors.Muted
        status == RsvpStatus.In -> MusterColors.InText
        status == RsvpStatus.Pending -> MusterColors.PendingText
        else -> MusterColors.OutText
    }
    val label = if (noReply) stringResource(Res.string.event_status_no_reply) else stringResource(status.label())
    val content: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = text)
            if (chevron) {
                Spacer(Modifier.width(2.dp))
                Text(text = "›", style = MaterialTheme.typography.labelSmall, color = text)
            }
        }
    }
    when {
        noReply -> Surface(
            modifier = modifier,
            shape = RoundedCornerShape(percent = 50),
            border = BorderStroke(1.dp, MusterColors.Outline),
            color = fill
        ) { content() }

        onClick != null -> Surface(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(percent = 50),
            color = fill
        ) { content() }

        else -> Surface(
            modifier = modifier,
            shape = RoundedCornerShape(percent = 50),
            color = fill
        ) { content() }
    }
}

private fun RsvpStatus.label() = when (this) {
    RsvpStatus.In -> Res.string.events_status_in
    RsvpStatus.Pending -> Res.string.events_status_pending
    RsvpStatus.Out -> Res.string.events_status_out
}

@Composable
private fun AddPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(percent = 50),
        color = MusterColors.InFill
    ) {
        Box(modifier = Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            Text(text = text, style = MaterialTheme.typography.labelLarge, color = MusterColors.InText)
        }
    }
}

@Preview
@Composable
private fun RsvpStatusBadgePreview() {
    MusterTheme {
        Surface {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                RsvpStatusBadge(status = RsvpStatus.In, isFrozen = false)
                Spacer(Modifier.width(8.dp))
                RsvpStatusBadge(status = RsvpStatus.Pending, isFrozen = false)
                Spacer(Modifier.width(8.dp))
                RsvpStatusBadge(status = RsvpStatus.Out, isFrozen = false)
                Spacer(Modifier.width(8.dp))
                RsvpStatusBadge(status = RsvpStatus.Pending, isFrozen = true)
                Spacer(Modifier.width(8.dp))
                RsvpStatusBadge(status = RsvpStatus.In, isFrozen = false, chevron = true, onClick = {})
            }
        }
    }
}

@Preview
@Composable
private fun RosterRowItemPreview() {
    MusterTheme {
        Surface {
            RosterRowItem(
                row = PreviewRoster[0].copy(actions = listOf(RosterAction.SetOut, RosterAction.RemoveFromEvent)),
                isFrozen = false,
                readOnly = false,
                inFlight = false,
                errorMessage = null,
                showDivider = true,
                onAction = {},
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Preview
@Composable
private fun RosterRowItemErrorPreview() {
    MusterTheme {
        Surface {
            RosterRowItem(
                row = PreviewRoster[2].copy(actions = listOf(RosterAction.SetOut, RosterAction.RemoveFromEvent)),
                isFrozen = false,
                readOnly = false,
                inFlight = false,
                errorMessage = DomainError.EventFull().toMessage(),
                showDivider = true,
                onAction = {},
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Preview
@Composable
private fun RosterSectionPreview() {
    MusterTheme {
        Surface {
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                rosterSection(
                    state = PreviewSuccess,
                    showAddAction = true,
                    isFrozen = false,
                    onAddPlayers = {},
                    onRowAction = { _, _ -> }
                )
            }
        }
    }
}

@Preview
@Composable
private fun RosterSectionEmptyAdminPreview() {
    MusterTheme {
        Surface {
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                rosterSection(
                    state = PreviewSuccess.copy(roster = emptyList()),
                    showAddAction = true,
                    isFrozen = false,
                    onAddPlayers = {},
                    onRowAction = { _, _ -> }
                )
            }
        }
    }
}

@Preview
@Composable
private fun RosterSectionEmptyMemberPreview() {
    MusterTheme {
        Surface {
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                rosterSection(
                    state = PreviewSuccess.copy(isAdmin = false, roster = emptyList()),
                    showAddAction = false,
                    isFrozen = false,
                    onAddPlayers = {},
                    onRowAction = { _, _ -> }
                )
            }
        }
    }
}
