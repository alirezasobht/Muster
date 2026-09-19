package app.muster.ui.screens.event

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.muster.domain.error.DomainError
import app.muster.domain.model.RsvpStatus
import app.muster.ui.common.components.FormError
import app.muster.ui.common.components.MessageState
import app.muster.ui.common.components.MusterIcons
import app.muster.ui.common.components.MusterSpinner
import app.muster.ui.common.components.PhoneWidth
import app.muster.ui.common.components.PrimaryButton
import app.muster.ui.common.toMessage
import app.muster.ui.common.util.SharedTransitionKeys
import app.muster.ui.common.util.sharedBoundsOrNone
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.action_cant_make_it
import muster.shared.generated.resources.action_change
import muster.shared.generated.resources.action_im_in
import muster.shared.generated.resources.content_description_back
import muster.shared.generated.resources.event_failed_title
import muster.shared.generated.resources.event_frozen_strip
import muster.shared.generated.resources.event_roster_empty_body
import muster.shared.generated.resources.event_roster_empty_title
import muster.shared.generated.resources.event_roster_header
import muster.shared.generated.resources.event_rsvp_frozen_in
import muster.shared.generated.resources.event_rsvp_frozen_out
import muster.shared.generated.resources.event_rsvp_frozen_pending
import muster.shared.generated.resources.event_rsvp_in
import muster.shared.generated.resources.event_rsvp_out
import muster.shared.generated.resources.event_rsvp_question
import muster.shared.generated.resources.event_slots
import muster.shared.generated.resources.event_slots_frozen
import muster.shared.generated.resources.event_status_no_reply
import muster.shared.generated.resources.event_try_again
import muster.shared.generated.resources.events_status_in
import muster.shared.generated.resources.events_status_out
import muster.shared.generated.resources.events_status_pending
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

data class EventActions(
    val onBack: () -> Unit,
    val onRsvp: (RsvpStatus) -> Unit,
    val onChangeRowStatus: (playerId: String, status: RsvpStatus) -> Unit,
    val onRetry: () -> Unit,
    val onRefresh: () -> Unit = {}
)

@Composable
fun EventRoute(
    initialSummary: EventSummary,
    onBack: () -> Unit,
    viewModel: EventViewModel = koinViewModel { parametersOf(initialSummary) }
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    EventScreen(
        state = state,
        actions = EventActions(
            onBack = onBack,
            onRsvp = viewModel::onRsvp,
            onChangeRowStatus = viewModel::onChangeRowStatus,
            onRetry = viewModel::onRetry,
            onRefresh = viewModel::onRefresh
        )
    )
}

@Composable
fun EventScreen(
    state: EventUiState,
    actions: EventActions,
    modifier: Modifier = Modifier,
    spinnerDelayMillis: Long = 400
) {
    Surface(modifier = modifier.fillMaxSize()) {
        PhoneWidth {
            Column(modifier = Modifier.fillMaxSize()) {
                // Known from the route, not the fetch — renders on every
                // branch below, including Loading and Error.
                EventAppBar(summary = state.summary, onBack = actions.onBack)
                when (state) {
                    is EventUiState.Loading -> {
                        HorizontalDivider(color = MusterColors.Hairline)
                        Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            EventDetails(summary = state.summary, isFrozen = false, modifier = Modifier.padding(24.dp))
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                MusterSpinner(delayMillis = spinnerDelayMillis)
                            }
                        }
                    }
                    is EventUiState.Error -> {
                        HorizontalDivider(color = MusterColors.Hairline)
                        Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            EventDetails(summary = state.summary, isFrozen = false, modifier = Modifier.padding(24.dp))
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                MessageState(
                                    title = stringResource(Res.string.event_failed_title),
                                    body = state.error.toMessage()
                                ) {
                                    OutlinedButton(
                                        onClick = actions.onRetry,
                                        shape = MaterialTheme.shapes.medium,
                                        border = BorderStroke(1.5.dp, MusterColors.InText),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MusterColors.InText),
                                        modifier = Modifier.fillMaxWidth().height(52.dp)
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.event_try_again),
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is EventUiState.Success -> EventContent(
                        state = state,
                        actions = actions,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EventAppBar(
    summary: EventSummary,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.padding(horizontal = 16.dp)) {
            Icon(
                imageVector = MusterIcons.ArrowBack,
                contentDescription = stringResource(Res.string.content_description_back),
                tint = MusterColors.Ink,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = summary.groupName.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MusterColors.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.sharedBoundsOrNone(SharedTransitionKeys.groupName(summary.groupId)).padding(vertical = 8.dp)
            )
            Text(
                text = summary.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.sharedBoundsOrNone(SharedTransitionKeys.eventTitle(summary.eventId)).padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun EventContent(
    state: EventUiState.Success,
    actions: EventActions,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (state.isFrozen) {
            Surface(color = MusterColors.QuietSurface) {
                Text(
                    text = stringResource(Res.string.event_frozen_strip, state.startTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MusterColors.Secondary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp)
                )
            }
        }
        HorizontalDivider(color = MusterColors.Hairline)
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = actions.onRefresh,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        Spacer(Modifier.height(16.dp))
                        EventDetails(summary = state.summary, isFrozen = state.isFrozen)
                        Spacer(Modifier.height(16.dp))
                        if (state.myStatus != null) {
                            OwnRsvpBlock(state = state, onRsvp = actions.onRsvp)
                            Spacer(Modifier.height(20.dp))
                        }
                        Text(
                            text = stringResource(Res.string.event_roster_header, state.roster.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = MusterColors.Muted
                        )
                    }
                }
                if (state.isRosterEmpty) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            MessageState(
                                title = stringResource(Res.string.event_roster_empty_title),
                                body = stringResource(Res.string.event_roster_empty_body)
                            )
                        }
                    }
                } else {
                    items(state.roster, key = { it.id }) { row ->
                        RosterRowItem(
                            row = row,
                            isAdmin = state.isAdmin,
                            isFrozen = state.isFrozen,
                            inFlight = state.rowActionTargetId == row.id,
                            errorMessage = state.rowActionError?.toMessage()
                                ?.takeIf { state.rowActionTargetId == row.id },
                            onChangeStatus = { status -> actions.onChangeRowStatus(row.id, status) },
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun EventDetails(summary: EventSummary, isFrozen: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = summary.date,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.sharedBoundsOrNone(SharedTransitionKeys.eventDate(summary.eventId))
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = summary.location,
            style = MaterialTheme.typography.bodyMedium,
            color = MusterColors.Secondary,
            modifier = Modifier.sharedBoundsOrNone(SharedTransitionKeys.eventLocation(summary.eventId))
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (isFrozen) {
                stringResource(
                    Res.string.event_slots_frozen,
                    summary.inCount + summary.pendingCount,
                    summary.capacity,
                    summary.inCount,
                    summary.pendingCount
                )
            } else {
                stringResource(
                    Res.string.event_slots,
                    summary.inCount + summary.pendingCount,
                    summary.capacity,
                    summary.inCount,
                    summary.pendingCount
                )
            },
            style = MaterialTheme.typography.bodySmall,
            color = MusterColors.Muted,
            modifier = Modifier.sharedBoundsOrNone(SharedTransitionKeys.eventStats(summary.eventId))
        )
    }
}

@Composable
private fun OwnRsvpBlock(
    state: EventUiState.Success,
    onRsvp: (RsvpStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val status = state.myStatus ?: return

    if (state.isFrozen) {
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
                        when (status) {
                            RsvpStatus.In -> Res.string.event_rsvp_frozen_in
                            RsvpStatus.Out -> Res.string.event_rsvp_frozen_out
                            RsvpStatus.Pending -> Res.string.event_rsvp_frozen_pending
                        }
                    ),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f)
                )
                RsvpStatusBadge(status = status, isFrozen = true)
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
                Row {
                    PrimaryButton(
                        text = stringResource(Res.string.action_im_in),
                        onClick = { isChanging = false; onRsvp(RsvpStatus.In) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(
                        onClick = { isChanging = false; onRsvp(RsvpStatus.Out) },
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MusterColors.Outline),
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.action_cant_make_it),
                            style = MaterialTheme.typography.labelLarge,
                            color = MusterColors.Ink
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

@Composable
private fun RosterRowItem(
    row: RosterRow,
    isAdmin: Boolean,
    isFrozen: Boolean,
    inFlight: Boolean,
    errorMessage: String?,
    onChangeStatus: (RsvpStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InitialsAvatar(row.name)
            Spacer(Modifier.width(12.dp))
            Text(
                text = row.name + if (row.isSelf) " · you" else "",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(8.dp))
            if (isAdmin && !isFrozen) {
                AdminStatusMenu(status = row.status, inFlight = inFlight, onChangeStatus = onChangeStatus)
            } else {
                RsvpStatusBadge(status = row.status, isFrozen = isFrozen)
            }
        }
        if (errorMessage != null) {
            Spacer(Modifier.height(6.dp))
            Text(text = errorMessage, style = MaterialTheme.typography.bodySmall, color = MusterColors.OutText)
        }
    }
}

@Composable
private fun AdminStatusMenu(
    status: RsvpStatus,
    inFlight: Boolean,
    onChangeStatus: (RsvpStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        RsvpStatusBadge(status = status, isFrozen = false, chevron = true, onClick = { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            RsvpStatus.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.label())) },
                    onClick = { expanded = false; onChangeStatus(option) },
                    enabled = !inFlight && option != status
                )
            }
        }
    }
}

@Composable
private fun RsvpStatusBadge(
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
private fun InitialsAvatar(name: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(40.dp),
        shape = RoundedCornerShape(percent = 50),
        color = MusterColors.AvatarFill
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = initials(name),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                color = MusterColors.Secondary
            )
        }
    }
}

// Cosmetic only — not a permission, safe to compute here. Duplicated from
// MembersTab rather than shared: same reasoning as that file's own copy.
private fun initials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> ""
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}

private val PreviewActions = EventActions(
    onBack = {},
    onRsvp = {},
    onChangeRowStatus = { _, _ -> },
    onRetry = {}
)

private val PreviewRoster = listOf(
    RosterRow(id = "1", name = "Alex Doyle", status = RsvpStatus.In),
    RosterRow(id = "2", name = "Dan Whelan", status = RsvpStatus.In),
    RosterRow(id = "3", name = "Sam Okafor", status = RsvpStatus.In),
    RosterRow(id = "4", name = "Tomás Neale", status = RsvpStatus.Pending),
    RosterRow(id = "5", name = "Marcus Keane", status = RsvpStatus.Out, isSelf = true)
)

private val PreviewSummary = EventSummary(
    groupId = "1",
    eventId = "2",
    groupName = "Westgate Wednesday 7s",
    title = "Weekly 7-a-side",
    date = "Wed 17 Sep · 7:00 pm",
    location = "Westgate Pitch 2",
    capacity = 10,
    inCount = 3,
    pendingCount = 1
)

private val PreviewSuccess = EventUiState.Success(
    summary = PreviewSummary,
    isAdmin = true,
    myStatus = RsvpStatus.Out,
    roster = PreviewRoster
)

@Preview
@Composable
private fun EventScreenAdminPreview() {
    MusterTheme {
        EventScreen(state = PreviewSuccess, actions = PreviewActions)
    }
}

@Preview
@Composable
private fun EventScreenMemberUnansweredPreview() {
    MusterTheme {
        EventScreen(
            state = PreviewSuccess.copy(isAdmin = false, myStatus = RsvpStatus.Pending),
            actions = PreviewActions
        )
    }
}

@Preview
@Composable
private fun EventScreenFrozenPreview() {
    MusterTheme {
        EventScreen(
            state = PreviewSuccess.copy(
                isFrozen = true,
                startTime = "7:00 pm",
                myStatus = RsvpStatus.In
            ),
            actions = PreviewActions
        )
    }
}

@Preview
@Composable
private fun EventScreenEmptyRosterPreview() {
    MusterTheme {
        EventScreen(
            state = PreviewSuccess.copy(
                summary = PreviewSummary.copy(inCount = 0, pendingCount = 0),
                roster = emptyList(),
                myStatus = null
            ),
            actions = PreviewActions
        )
    }
}

// Shows what changed: the app bar and details render immediately from the
// carried-over summary, only the roster area waits on the fetch.
@Preview
@Composable
private fun EventScreenLoadingPreview() {
    MusterTheme {
        EventScreen(
            state = EventUiState.Loading(PreviewSummary),
            actions = PreviewActions,
            spinnerDelayMillis = 0
        )
    }
}
