package app.muster.ui.screens.event

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
import kotlin.math.roundToInt

data class EventActions(
    val onBack: () -> Unit,
    val onRsvp: (RsvpStatus) -> Unit,
    val onChangeRowStatus: (playerId: String, status: RsvpStatus) -> Unit,
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
            onChangeRowStatus = viewModel::onChangeRowStatus,
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
        // A standby drag is itself a vertical drag, same axis the list
        // scrolls on — without this the list scrolls under the finger
        // while a row is being dragged.
        var standbyDragging by remember { mutableStateOf(false) }
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = actions.onRefresh,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize(), userScrollEnabled = !standbyDragging) {
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
                if (state.standby.isNotEmpty()) {
                    item {
                        StandbySection(
                            standby = state.standby,
                            isAdmin = state.isAdmin,
                            isFrozen = state.isFrozen,
                            reordering = state.standbyReordering,
                            errorMessage = state.standbyError?.toMessage(),
                            onDraggingChanged = { standbyDragging = it },
                            onReorder = actions.onReorderStandby,
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

private val StandbyRowHeight = 48.dp

@Composable
private fun StandbySection(
    standby: List<StandbyRow>,
    isAdmin: Boolean,
    isFrozen: Boolean,
    reordering: Boolean,
    errorMessage: String?,
    onDraggingChanged: (Boolean) -> Unit,
    onReorder: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val reorderable = isAdmin && !isFrozen
    Column(modifier = modifier) {
        Spacer(Modifier.height(20.dp))
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
        Spacer(Modifier.height(8.dp))
        if (reorderable) {
            // A drag dropped while the previous one is still in flight would
            // race the same queue write — disabled, not queued, until it
            // settles.
            ReorderableStandbyList(
                standby = standby,
                enabled = !reordering,
                onDraggingChanged = onDraggingChanged,
                onReorder = onReorder
            )
        } else {
            Column {
                standby.forEachIndexed { index, row ->
                    StandbyRowItem(position = index + 1, row = row)
                }
            }
        }
        if (errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(text = errorMessage, style = MaterialTheme.typography.bodySmall, color = MusterColors.OutText)
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

// Hand-rolled: no drag-reorder library in this project. A LazyColumn with
// scrolling off, not a plain Column — Compose only animates a reordered
// item into its new slot (`animateItem`) when items carry a stable `key`,
// and only LazyColumn supports item keys.
//
// `standby` itself is only ever read here, never copied — the caller (the
// ViewModel) already reorders it optimistically the instant a drop lands,
// before the write round-trips, so what's on screen never has to wait on
// the network to look right. A failed write reverts it there too.
@Composable
private fun ReorderableStandbyList(
    standby: List<StandbyRow>,
    enabled: Boolean,
    onDraggingChanged: (Boolean) -> Unit,
    onReorder: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val rowHeightPx = with(density) { StandbyRowHeight.toPx() }
    val dragHandleDescription = stringResource(Res.string.event_standby_drag_hint)

    val draggedIndex = standby.indexOfFirst { it.id == draggedId }
    // Where the drag would land if dropped right now — for the visual
    // make-room offsets below. Recomputed from live state each frame, not
    // captured, since onDragEnd needs this same formula fresh at drop time.
    val targetIndex = if (draggedIndex < 0) {
        -1
    } else {
        (draggedIndex + (dragOffset / rowHeightPx).roundToInt()).coerceIn(0, standby.lastIndex)
    }

    LazyColumn(modifier = modifier.height(StandbyRowHeight * standby.size), userScrollEnabled = false) {
        itemsIndexed(standby, key = { _, row -> row.id }) { index, row ->
            val isDragged = row.id == draggedId
            val rawOffset = when {
                draggedIndex < 0 -> 0f
                isDragged -> dragOffset
                draggedIndex < targetIndex && index in (draggedIndex + 1)..targetIndex -> -rowHeightPx
                draggedIndex > targetIndex && index in targetIndex until draggedIndex -> rowHeightPx
                else -> 0f
            }
            // Spring only while a drag is in progress, so making room reads
            // as a shift. The moment the drop lands the list has reordered
            // and every offset is stale by exactly one row — snap clears them
            // instantly instead of sliding rows in from where they used to be.
            val animatedOffset by animateFloatAsState(
                targetValue = if (isDragged) 0f else rawOffset,
                animationSpec = if (draggedId != null) spring() else snap(),
                label = "standbyRowOffset"
            )
            val offset = if (isDragged) dragOffset else animatedOffset

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(StandbyRowHeight)
                    .zIndex(if (isDragged) 1f else 0f)
                    // No animateItem(): the make-room shifts below are already
                    // moving these rows with translationY, and letting Compose
                    // animate the slot change as well adds the two distances
                    // together — rows overshoot their new position on drop.
                    .graphicsLayer { translationY = offset }
                    .then(
                        if (isDragged) {
                            Modifier
                                .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp))
                                .background(MusterColors.White, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp)
                        } else {
                            Modifier
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = (index + 1).toString(),
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
                val dragModifier = if (!enabled) {
                    Modifier
                } else {
                    Modifier.pointerInput(row.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggedId = row.id
                                dragOffset = 0f
                                onDraggingChanged(true)
                            },
                            onDragEnd = {
                                // Fresh read, not the outer `targetIndex` val
                                // — that one was captured when this gesture
                                // block was set up and won't have moved.
                                val from = standby.indexOfFirst { it.id == draggedId }
                                val to = if (from < 0) {
                                    -1
                                } else {
                                    (from + (dragOffset / rowHeightPx).roundToInt()).coerceIn(0, standby.lastIndex)
                                }
                                draggedId = null
                                dragOffset = 0f
                                onDraggingChanged(false)
                                if (from != to && from in standby.indices && to in standby.indices) {
                                    val reordered = standby.toMutableList().apply { add(to, removeAt(from)) }
                                    onReorder(reordered.map { it.id })
                                }
                            },
                            onDragCancel = {
                                draggedId = null
                                dragOffset = 0f
                                onDraggingChanged(false)
                            },
                            onDrag = { change, delta ->
                                change.consume()
                                if (draggedId == null) return@detectDragGesturesAfterLongPress
                                dragOffset += delta.y
                            }
                        )
                    }
                }
                Icon(
                    imageVector = MusterIcons.DragHandle,
                    contentDescription = dragHandleDescription,
                    tint = if (enabled) MusterColors.Hint else MusterColors.Hairline,
                    modifier = Modifier
                        .size(32.dp)
                        .then(dragModifier)
                )
            }
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
