package app.muster.ui.screens.event

import app.muster.ui.common.components.MusterPullToRefreshBox
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import muster.shared.generated.resources.event_action_cancel_invite
import muster.shared.generated.resources.event_action_remove_from_event
import muster.shared.generated.resources.event_action_remove_from_list
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
import muster.shared.generated.resources.event_standby_drag_hint
import muster.shared.generated.resources.event_standby_header
import muster.shared.generated.resources.event_standby_header_frozen
import muster.shared.generated.resources.event_status_no_reply
import muster.shared.generated.resources.event_try_again
import muster.shared.generated.resources.events_status_in
import muster.shared.generated.resources.events_status_out
import muster.shared.generated.resources.events_status_pending
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import sh.calvin.reorderable.ReorderableColumn

data class EventActions(
    val onBack: () -> Unit,
    val onRsvp: (RsvpStatus) -> Unit,
    val onRosterAction: (playerId: String, action: RosterAction) -> Unit,
    // Send the whole ordered queue
    val onReorderStandby: (orderedIds: List<String>) -> Unit,
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
            onRosterAction = viewModel::onRosterAction,
            onReorderStandby = viewModel::onReorderStandby,
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

        var standbyDragging by remember { mutableStateOf(false) }
        val refreshEnabled = !standbyDragging && !state.standbyReordering

        MusterPullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { if (refreshEnabled) actions.onRefresh() },
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp),
                userScrollEnabled = !standbyDragging
            ) {

                item { Spacer(Modifier.height(16.dp)) }
                item { EventDetails(summary = state.summary, isFrozen = state.isFrozen) }
                item { Spacer(Modifier.height(16.dp)) }
                if (state.myStatus != null) {
                    item { OwnRsvpBlock(state = state, onRsvp = actions.onRsvp) }
                    item { Spacer(Modifier.height(20.dp)) }
                }
                item {
                    Text(
                        text = stringResource(Res.string.event_roster_header, state.roster.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MusterColors.Muted
                    )
                }

                if (state.isRosterEmpty) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
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
                            isFrozen = state.isFrozen,
                            inFlight = state.rowActionTargetId == row.id,
                            errorMessage = state.rowActionError?.toMessage()
                                ?.takeIf { state.rowActionTargetId == row.id },
                            onAction = { action -> actions.onRosterAction(row.id, action) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }

                if (state.standby.isNotEmpty()) {
                    standbySection(
                        standby = state.standby,
                        isAdmin = state.isAdmin,
                        isFrozen = state.isFrozen,
                        reordering = state.standbyReordering,
                        error = state.standbyError,
                        onDraggingChanged = { standbyDragging = it },
                        onReorder = actions.onReorderStandby
                    )
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
    isFrozen: Boolean,
    inFlight: Boolean,
    errorMessage: String?,
    onAction: (RosterAction) -> Unit,
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
            StatusMenu(
                status = row.status,
                inFlight = inFlight,
                rowActions = row.actions,
                isFrozen = isFrozen,
                onAction = onAction
            )
        }
        if (errorMessage != null) {
            Spacer(Modifier.height(6.dp))
            Text(text = errorMessage, style = MaterialTheme.typography.bodySmall, color = MusterColors.OutText)
        }
    }
}

private val StandbyRowHeight = 48.dp

private fun LazyListScope.standbySection(
    standby: List<StandbyRow>,
    isAdmin: Boolean,
    isFrozen: Boolean,
    reordering: Boolean,
    error: DomainError?,
    onDraggingChanged: (Boolean) -> Unit,
    onReorder: (List<String>) -> Unit
) {
    val reorderable = isAdmin && !isFrozen
    item { Spacer(Modifier.height(20.dp)) }
    item {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isFrozen) {
                    stringResource(Res.string.event_standby_header_frozen)
                } else {
                    stringResource(Res.string.event_standby_header, standby.size)
                },
                style = MaterialTheme.typography.labelMedium,
                color = MusterColors.Muted,
                modifier = Modifier.weight(1f)
            )
            if (reorderable) {
                Text(
                    text = stringResource(Res.string.event_standby_drag_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MusterColors.Muted
                )
            }
        }
    }
    item { Spacer(Modifier.height(8.dp)) }
    if (reorderable) {
        item {
            ReorderableStandbyList(
                standby = standby,
                enabled = !reordering,
                onDraggingChanged = onDraggingChanged,
                onReorder = onReorder
            )
        }
    } else {
        itemsIndexed(standby, key = { _, row -> row.id }) { index, row ->
            StandbyRowItem(
                position = index + 1,
                row = row,
                modifier = Modifier.animateItem()
            )
        }
    }
    error?.let {
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it.toMessage(),
                style = MaterialTheme.typography.bodySmall,
                color = MusterColors.OutText
            )
        }
    }
}

@Composable
private fun StandbyRowItem(position: Int, row: StandbyRow, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().height(StandbyRowHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = position.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MusterColors.Muted,
            modifier = Modifier.width(24.dp)
        )
        Text(
            text = row.name + if (row.isSelf) " · you" else "",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ReorderableStandbyList(
    standby: List<StandbyRow>,
    enabled: Boolean,
    onDraggingChanged: (Boolean) -> Unit,
    onReorder: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val dragHandleDescription = stringResource(Res.string.event_standby_drag_hint)

    ReorderableColumn(
        list = standby,
        onSettle = { from, to ->
            if (from != to && from in standby.indices && to in standby.indices) {
                val reordered = standby.toMutableList().apply { add(to, removeAt(from)) }
                onReorder(reordered.map { it.id })
            }
        },
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) { index, row, isDragging ->
        key(row.id) {
            ReorderableItem {
                val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)
                val borderColor by animateColorAsState(if (isDragging) MusterColors.Hint else MusterColors.Hairline)
                val dragModifier = if (!enabled) {
                    Modifier
                } else {
                    Modifier.draggableHandle(
                        onDragStarted = { onDraggingChanged(true) },
                        onDragStopped = { onDraggingChanged(false) }
                    )
                }
                Card(
                    onClick = {},
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = elevation,
                        pressedElevation = elevation,
                        draggedElevation = elevation,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MusterColors.White),
                    border = BorderStroke(1.dp, borderColor),

                    ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(StandbyRowHeight),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = (index + 1).toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MusterColors.Muted,
                            modifier = Modifier.width(24.dp),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = row.name + if (row.isSelf) " · you" else "",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = {},
                            modifier = dragModifier,
                            enabled = enabled
                        ) {
                            Icon(
                                imageVector = MusterIcons.DragHandle,
                                contentDescription = dragHandleDescription,
                                tint = if (enabled) MusterColors.Hint else MusterColors.Hairline,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusMenu(
    status: RsvpStatus,
    inFlight: Boolean,
    rowActions: List<RosterAction>,
    isFrozen: Boolean,
    onAction: (RosterAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val hasAction = rowActions.isNotEmpty() && !isFrozen
    Box(modifier = modifier) {
        RsvpStatusBadge(status = status, isFrozen = isFrozen, chevron = hasAction, onClick = { expanded = true })
        if (hasAction) {
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                rowActions.forEach { action ->
                    DropdownMenuItem(
                        text = { Text(text = stringResource(action.label())) },
                        onClick = { expanded = false; onAction(action) },
                        enabled = !inFlight && !isFrozen
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
    onRosterAction = { _, _ -> },
    onReorderStandby = {},
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

private val PreviewStandby = listOf(
    StandbyRow(id = "6", name = "Joe Moriarty"),
    StandbyRow(id = "7", name = "Priya Nair", isSelf = true),
    StandbyRow(id = "8", name = "Leo Fanning")
)

// Admin: drag handles and the "Drag to reorder" hint.
@Preview
@Composable
private fun EventScreenAdminWithStandbyPreview() {
    MusterTheme {
        EventScreen(
            state = PreviewSuccess.copy(isAdmin = true, standby = PreviewStandby),
            actions = PreviewActions
        )
    }
}

// Member: plain numbered list, no handles, no hint.
@Preview
@Composable
private fun EventScreenMemberWithStandbyPreview() {
    MusterTheme {
        EventScreen(
            state = PreviewSuccess.copy(isAdmin = false, standby = PreviewStandby),
            actions = PreviewActions
        )
    }
}

// An empty queue: the section doesn't render at all, not an empty-state line.
@Preview
@Composable
private fun EventScreenStandbyEmptyPreview() {
    MusterTheme {
        EventScreen(
            state = PreviewSuccess.copy(isAdmin = true, standby = emptyList()),
            actions = PreviewActions
        )
    }
}

// Frozen: "Not called up" header, plain list, no handles even for admins.
@Preview
@Composable
private fun EventScreenFrozenWithStandbyPreview() {
    MusterTheme {
        EventScreen(
            state = PreviewSuccess.copy(
                isAdmin = true,
                isFrozen = true,
                startTime = "7:00 pm",
                myStatus = RsvpStatus.In,
                standby = PreviewStandby
            ),
            actions = PreviewActions
        )
    }
}
