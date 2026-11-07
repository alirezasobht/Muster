package app.muster.ui.screens.event

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.muster.domain.error.DomainError
import app.muster.domain.model.RsvpStatus
import app.muster.ui.common.components.MessageState
import app.muster.ui.common.components.MusterPullToRefreshBox
import app.muster.ui.common.components.MusterSpinner
import app.muster.ui.common.components.PhoneWidth
import app.muster.ui.common.toMessage
import app.muster.ui.screens.event.components.EventAppBar
import app.muster.ui.screens.event.components.EventDetails
import app.muster.ui.screens.event.components.EventDialogs
import app.muster.ui.screens.event.components.EventPhaseStrip
import app.muster.ui.screens.event.components.PendingRosterConfirmation
import app.muster.ui.screens.event.components.PreviewActions
import app.muster.ui.screens.event.components.PreviewStandby
import app.muster.ui.screens.event.components.PreviewSuccess
import app.muster.ui.screens.event.components.PreviewSummary
import app.muster.ui.screens.event.components.confirmationDialog
import app.muster.ui.screens.event.components.detailsSection
import app.muster.ui.screens.event.components.ownRsvpSection
import app.muster.ui.screens.event.components.rosterSection
import app.muster.ui.screens.event.components.standbySection
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import kotlin.time.Instant
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.event_failed_title
import muster.shared.generated.resources.event_try_again
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

data class EventActions(
    val onBack: () -> Unit,
    val onRsvp: (RsvpStatus) -> Unit,
    val onRosterAction: (playerId: String, action: RosterAction) -> Unit,
    // Send the whole ordered queue
    val onReorderStandby: (orderedIds: List<String>) -> Unit,
    val onRetry: () -> Unit,
    val onRefresh: () -> Unit = {},
    val onAddPlayers: () -> Unit = {},
    val onOverflowAction: (EventOverflowAction) -> Unit = {},
    val onLockToggled: (locked: Boolean) -> Unit = {},
    val onDialogConfirmed: () -> Unit = {},
    val onDialogDismissed: () -> Unit = {}
)

@Composable
fun EventRoute(
    initialSummary: EventSummary,
    onBack: () -> Unit,
    onAddPlayers: (eventId: String, groupId: String) -> Unit,
    onEditEvent: (summary: EventSummary, startsAt: Instant) -> Unit,
    onDeleted: () -> Unit,
    viewModel: EventViewModel = koinViewModel { parametersOf(initialSummary) }
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deleted = (state as? EventUiState.Success)?.deleted == true
    LaunchedEffect(deleted) {
        if (deleted) {
            onDeleted()
            viewModel.onDeletedHandled()
        }
    }
    EventScreen(
        state = state,
        actions = EventActions(
            onBack = onBack,
            onRsvp = viewModel::onRsvp,
            onRosterAction = viewModel::onRosterAction,
            onReorderStandby = viewModel::onReorderStandby,
            onRetry = viewModel::onRetry,
            onRefresh = viewModel::onRefresh,
            onAddPlayers = { onAddPlayers(state.summary.eventId, state.summary.groupId) },
            onOverflowAction = { action ->
                when (action) {
                    EventOverflowAction.Edit ->
                        (state as? EventUiState.Success)?.let { onEditEvent(it.summary, it.startsAt) }
                    EventOverflowAction.Cancel -> viewModel.onCancelRequested()
                    EventOverflowAction.Delete -> viewModel.onDeleteRequested()
                }
            },
            onLockToggled = viewModel::onLockToggled,
            onDialogConfirmed = viewModel::onDialogConfirmed,
            onDialogDismissed = viewModel::onDialogDismissed
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
                EventAppBar(
                    summary = state.summary,
                    overflowActions = (state as? EventUiState.Success)?.overflowActions ?: emptyList(),
                    onBack = actions.onBack,
                    onOverflowAction = actions.onOverflowAction
                )
                when (state) {
                    is EventUiState.Loading -> EventLoading(
                        summary = state.summary,
                        spinnerDelayMillis = spinnerDelayMillis,
                        modifier = Modifier.weight(1f)
                    )

                    is EventUiState.Error -> EventError(
                        summary = state.summary,
                        error = state.error,
                        onRetry = actions.onRetry,
                        modifier = Modifier.weight(1f)
                    )

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
private fun EventLoading(
    summary: EventSummary,
    spinnerDelayMillis: Long,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = MusterColors.Hairline)
        EventDetails(summary = summary, isFrozen = false, modifier = Modifier.padding(24.dp))
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            MusterSpinner(delayMillis = spinnerDelayMillis)
        }
    }
}

@Composable
private fun EventError(
    summary: EventSummary,
    error: DomainError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = MusterColors.Hairline)
        EventDetails(summary = summary, isFrozen = false, modifier = Modifier.padding(24.dp))
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            MessageState(
                title = stringResource(Res.string.event_failed_title),
                body = error.toMessage()
            ) {
                OutlinedButton(
                    onClick = onRetry,
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

@Composable
private fun EventContent(
    state: EventUiState.Success,
    actions: EventActions,
    modifier: Modifier = Modifier
) {
    var pendingConfirmation by remember(state.isReadOnly) { mutableStateOf<PendingRosterConfirmation?>(null) }
    val onRowAction: (RosterRow, RosterAction) -> Unit = { row, action ->
        val dialog = action.confirmationDialog()
        if (dialog != null) {
            pendingConfirmation = PendingRosterConfirmation(row, dialog) { actions.onRosterAction(row.id, action) }
        } else {
            actions.onRosterAction(row.id, action)
        }
    }

    EventBody(state = state, actions = actions, onRowAction = onRowAction, modifier = modifier)

    EventDialogs(
        pendingConfirmation = pendingConfirmation,
        onPendingClosed = { pendingConfirmation = null },
        dialog = state.dialog,
        summary = state.summary,
        onConfirm = actions.onDialogConfirmed,
        onDismiss = actions.onDialogDismissed
    )
}

@Composable
private fun EventBody(
    state: EventUiState.Success,
    actions: EventActions,
    onRowAction: (RosterRow, RosterAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        EventPhaseStrip(phase = state.phase)

        val listState = rememberLazyListState()
        val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 } }
        if (scrolled) HorizontalDivider(color = MusterColors.Hairline)

        // A drag cut off by a lock or freeze may never report its stop.
        var standbyDragging by remember(state.isReadOnly) { mutableStateOf(false) }
        val refreshEnabled = !standbyDragging && !state.standbyReordering

        MusterPullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { if (refreshEnabled) actions.onRefresh() },
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp),
                userScrollEnabled = !standbyDragging
            ) {
                item { Spacer(Modifier.height(16.dp)) }
                detailsSection(state = state, onLockToggled = actions.onLockToggled)
                ownRsvpSection(state = state, onRsvp = actions.onRsvp)
                rosterSection(
                    state = state,
                    showAddAction = state.canAddPlayers,
                    isFrozen = state.isStarted,
                    onAddPlayers = actions.onAddPlayers,
                    onRowAction = onRowAction
                )
                standbySection(
                    standby = state.standby,
                    isAdmin = state.isAdmin,
                    phase = state.phase,
                    reordering = state.standbyReordering,
                    error = state.standbyError,
                    onDraggingChanged = { standbyDragging = it },
                    onReorder = actions.onReorderStandby
                )
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

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
                lockFeatureFlag = false,
                phase = EventPhase.Started(atDisplay = "7:00 pm"),
                myStatus = RsvpStatus.In
            ),
            actions = PreviewActions
        )
    }
}

@Preview
@Composable
private fun EventScreenAdminLockedPreview() {
    MusterTheme {
        EventScreen(
            state = PreviewSuccess.copy(
                phase = EventPhase.Locked(byName = "Ali"),
                myStatus = RsvpStatus.In,
                standby = PreviewStandby
            ),
            actions = PreviewActions
        )
    }
}

@Preview
@Composable
private fun EventScreenMemberLockedPreview() {
    MusterTheme {
        EventScreen(
            state = PreviewSuccess.copy(
                isAdmin = false,
                lockFeatureFlag = false,
                phase = EventPhase.Locked(byName = null),
                myStatus = null,
                standby = PreviewStandby
            ),
            actions = PreviewActions
        )
    }
}

@Preview
@Composable
private fun EventScreenAdminCancelledPreview() {
    MusterTheme {
        EventScreen(
            state = PreviewSuccess.copy(
                phase = EventPhase.Cancelled(byName = "Ali", started = false),
                myStatus = RsvpStatus.Pending,
                standby = PreviewStandby
            ),
            actions = PreviewActions
        )
    }
}

@Preview
@Composable
private fun EventScreenMemberCancelledPreview() {
    MusterTheme {
        EventScreen(
            state = PreviewSuccess.copy(
                isAdmin = false,
                phase = EventPhase.Cancelled(byName = null, started = false),
                myStatus = RsvpStatus.In,
                standby = PreviewStandby
            ),
            actions = PreviewActions
        )
    }
}

@Preview
@Composable
private fun EventScreenCancelDialogPreview() {
    MusterTheme {
        EventScreen(
            state = PreviewSuccess.copy(dialog = EventDialogState(EventDialogKind.Cancel)),
            actions = PreviewActions
        )
    }
}

@Preview
@Composable
private fun EventScreenDeleteDialogPreview() {
    MusterTheme {
        EventScreen(
            state = PreviewSuccess.copy(dialog = EventDialogState(EventDialogKind.Delete)),
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

@Preview
@Composable
private fun EventScreenRowErrorPreview() {
    MusterTheme {
        EventScreen(
            state = PreviewSuccess.copy(
                rowActionError = DomainError.EventFull(),
                rowActionErrorId = "3"
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
            state = PreviewSuccess.copy(isAdmin = true, standby = PreviewStandby),
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
                lockFeatureFlag = false,
                phase = EventPhase.Started(atDisplay = "7:00 pm"),
                myStatus = RsvpStatus.In,
                standby = PreviewStandby
            ),
            actions = PreviewActions
        )
    }
}
