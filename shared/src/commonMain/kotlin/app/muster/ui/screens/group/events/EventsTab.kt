package app.muster.ui.screens.group.events

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.muster.domain.error.DomainError
import app.muster.domain.model.Event
import app.muster.domain.model.emptyEvent
import app.muster.ui.common.components.MessageState
import app.muster.ui.common.components.MusterPullToRefreshBox
import app.muster.ui.common.components.MusterSpinner
import app.muster.ui.common.toMessage
import app.muster.ui.common.util.SharedTransitionKeys
import app.muster.ui.common.util.sharedBoundsOrNone
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.events_empty_body
import muster.shared.generated.resources.events_empty_title
import muster.shared.generated.resources.events_failed_title
import muster.shared.generated.resources.events_new
import muster.shared.generated.resources.events_slots
import muster.shared.generated.resources.events_status_in
import muster.shared.generated.resources.events_status_out
import muster.shared.generated.resources.events_status_pending
import muster.shared.generated.resources.events_try_again
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

data class EventsActions(
    val onSelectEvent: (event: Event) -> Unit,
    val getEvent: (id: String) -> Event,
    val onNewEvent: () -> Unit,
    val onRetry: () -> Unit,
    val onRefresh: () -> Unit = {}
)

@Composable
fun EventsRoute(
    groupId: String,
    onSelectEvent: (Event) -> Unit,
    onNewEvent: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventsViewModel = koinViewModel { parametersOf(groupId) }
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    val state by viewModel.state.collectAsStateWithLifecycle()
    EventsTab(
        state = state,
        actions = EventsActions(
            onSelectEvent = onSelectEvent,
            getEvent = viewModel::getEvent,
            onNewEvent = onNewEvent,
            onRetry = viewModel::onRetry,
            onRefresh = viewModel::onRefresh
        ),
        modifier = modifier
    )
}

@Composable
fun EventsTab(
    state: EventsUiState,
    actions: EventsActions,
    modifier: Modifier = Modifier,
    spinnerDelayMillis: Long = 400
) {
    when (state) {
        is EventsUiState.Loading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            MusterSpinner(delayMillis = spinnerDelayMillis)
        }

        is EventsUiState.Error -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            MessageState(
                title = stringResource(Res.string.events_failed_title),
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
                        text = stringResource(Res.string.events_try_again),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        is EventsUiState.Success -> EventsContent(
            state = state,
            actions = actions,
            modifier = modifier
        )
    }
}

@Composable
private fun EventsContent(
    state: EventsUiState.Success,
    actions: EventsActions,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        MusterPullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = actions.onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            if (state.isEmpty) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            MessageState(
                                title = stringResource(Res.string.events_empty_title),
                                body = stringResource(Res.string.events_empty_body)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(state.eventRows, key = { it.id }) { eventRow ->
                        EventRowItem(
                            eventRow = eventRow,
                            onClick = { actions.onSelectEvent(actions.getEvent(eventRow.id)) }
                        )
                    }
                }
            }
        }

        if (state.canCreateEvent) {
            val newEventLabel = stringResource(Res.string.events_new)
            ExtendedFloatingActionButton(
                onClick = actions.onNewEvent,
                icon = { Text(text = "+", style = MaterialTheme.typography.titleMedium) },
                text = { Text(newEventLabel) },
                containerColor = MusterColors.Accent,
                contentColor = MusterColors.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .semantics { contentDescription = newEventLabel }
            )
        }
    }
}

@Composable
private fun EventRowItem(
    eventRow: EventRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, MusterColors.Outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = eventRow.date,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MusterColors.Muted,
                    modifier = Modifier.sharedBoundsOrNone(SharedTransitionKeys.eventDate(eventRow.id))
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = eventRow.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.sharedBoundsOrNone(SharedTransitionKeys.eventTitle(eventRow.id))
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = eventRow.location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusterColors.Secondary,
                    modifier = Modifier.sharedBoundsOrNone(SharedTransitionKeys.eventLocation(eventRow.id))
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(
                        Res.string.events_slots,
                        eventRow.inCount + eventRow.pendingCount,
                        eventRow.capacity,
                        eventRow.inCount,
                        eventRow.pendingCount
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MusterColors.Muted,
                    modifier = Modifier.sharedBoundsOrNone(SharedTransitionKeys.eventStats(eventRow.id))
                )
            }
            if (eventRow.status != null) {
                Spacer(Modifier.width(12.dp))
                EventStatusBadge(status = eventRow.status)
            }
        }
    }
}

@Composable
private fun EventStatusBadge(
    status: MemberEventStatus,
    modifier: Modifier = Modifier
) {
    val fill = when (status) {
        MemberEventStatus.In -> MusterColors.InFill
        MemberEventStatus.Pending -> MusterColors.PendingFill
        MemberEventStatus.Out -> MusterColors.OutFill
    }
    val text = when (status) {
        MemberEventStatus.In -> MusterColors.InText
        MemberEventStatus.Pending -> MusterColors.PendingText
        MemberEventStatus.Out -> MusterColors.OutText
    }
    val label = stringResource(
        when (status) {
            MemberEventStatus.In -> Res.string.events_status_in
            MemberEventStatus.Pending -> Res.string.events_status_pending
            MemberEventStatus.Out -> Res.string.events_status_out
        }
    )
    Surface(modifier = modifier, shape = RoundedCornerShape(percent = 50), color = fill) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private val PreviewActions = EventsActions(
    onSelectEvent = {},
    getEvent = { Event.emptyEvent(id = it) },
    onNewEvent = {},
    onRetry = {}
)

private val AdminPreviewEvents = listOf(
    EventRow(
        id = "1",
        title = "Weekly 7-a-side",
        date = "Wed 17 Sep · 7:00 pm",
        location = "Westgate Pitch 2",
        capacity = 10,
        inCount = 7,
        pendingCount = 3,
        status = MemberEventStatus.In
    ),
    EventRow(
        id = "2",
        title = "Weekly 7-a-side",
        date = "Wed 24 Sep · 7:00 pm",
        location = "Westgate Pitch 2",
        capacity = 10,
        inCount = 4,
        pendingCount = 2,
        status = MemberEventStatus.Pending
    ),
    EventRow(
        id = "3",
        title = "Friendly vs Brunswick",
        date = "Sun 28 Sep · 10:00 am",
        location = "Clifton Park",
        capacity = 12,
        inCount = 6,
        pendingCount = 3,
        status = MemberEventStatus.Out
    )
)

@Preview
@Composable
private fun EventsTabAdminPreview() {
    MusterTheme {
        Surface {
            EventsTab(
                state = EventsUiState.Success(eventRows = AdminPreviewEvents, canCreateEvent = true),
                actions = PreviewActions
            )
        }
    }
}

@Preview
@Composable
private fun EventsTabMemberPreview() {
    MusterTheme {
        Surface {
            EventsTab(
                state = EventsUiState.Success(eventRows = AdminPreviewEvents, canCreateEvent = false),
                actions = PreviewActions
            )
        }
    }
}

@Preview
@Composable
private fun EventsTabEmptyPreview() {
    MusterTheme {
        Surface {
            EventsTab(
                state = EventsUiState.Success(eventRows = emptyList(), canCreateEvent = true),
                actions = PreviewActions
            )
        }
    }
}

@Preview
@Composable
private fun EventsTabLoadingPreview() {
    MusterTheme {
        Surface {
            EventsTab(
                state = EventsUiState.Loading,
                actions = PreviewActions,
                spinnerDelayMillis = 0
            )
        }
    }
}

@Preview
@Composable
private fun EventsTabFailedPreview() {
    MusterTheme {
        Surface {
            EventsTab(
                state = EventsUiState.Error(DomainError.Network()),
                actions = PreviewActions,
                spinnerDelayMillis = 0
            )
        }
    }
}
