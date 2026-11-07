package app.muster.ui.screens.event.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.muster.domain.error.DomainError
import app.muster.ui.common.components.MusterIcons
import app.muster.ui.common.toMessage
import app.muster.ui.screens.event.EventPhase
import app.muster.ui.screens.event.StandbyRow
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.event_standby_drag_hint
import muster.shared.generated.resources.event_standby_header
import muster.shared.generated.resources.event_standby_header_frozen
import muster.shared.generated.resources.event_standby_header_locked
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableColumn

private val StandbyRowHeight = 48.dp
private val StandbyDiscSize = 28.dp

internal fun LazyListScope.standbySection(
    standby: List<StandbyRow>,
    isAdmin: Boolean,
    phase: EventPhase,
    reordering: Boolean,
    error: DomainError?,
    onDraggingChanged: (Boolean) -> Unit,
    onReorder: (List<String>) -> Unit
) {
    if (standby.isEmpty()) return
    val reorderable = isAdmin && phase == EventPhase.Active
    item { Spacer(Modifier.height(20.dp)) }
    item {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when (phase) {
                    is EventPhase.Started, is EventPhase.Cancelled -> stringResource(Res.string.event_standby_header_frozen)
                    is EventPhase.Locked -> stringResource(Res.string.event_standby_header_locked)
                    EventPhase.Active -> stringResource(Res.string.event_standby_header, standby.size)
                },
                style = MaterialTheme.typography.titleMedium,
                color = MusterColors.Ink,
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
    item {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MusterColors.QuietSurface
        ) {
            Column {
                if (reorderable) {
                    ReorderableStandbyList(
                        standby = standby,
                        enabled = !reordering,
                        onDraggingChanged = onDraggingChanged,
                        onReorder = onReorder
                    )
                } else {
                    standby.forEachIndexed { index, row ->
                        StandbyRowItem(position = index + 1, row = row)
                    }
                }
            }
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
private fun StandbyPositionDisc(
    position: Int,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.size(StandbyDiscSize), shape = CircleShape, color = MusterColors.White) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = position.toString(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MusterColors.Ink
            )
        }
    }
}

@Composable
private fun StandbyRowItem(
    position: Int,
    row: StandbyRow,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().height(StandbyRowHeight).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StandbyPositionDisc(position)
        Spacer(Modifier.width(12.dp))
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
        modifier = modifier
    ) { index, row, isDragging ->
        key(row.id) {
            ReorderableItem {
                val dragModifier = if (!enabled) {
                    Modifier
                } else {
                    Modifier.draggableHandle(
                        onDragStarted = { onDraggingChanged(true) },
                        onDragStopped = { onDraggingChanged(false) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().height(StandbyRowHeight).padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StandbyPositionDisc(index + 1)
                    Spacer(Modifier.width(12.dp))
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

@Preview
@Composable
private fun ReorderableStandbyListPreview() {
    MusterTheme {
        Surface {
            ReorderableStandbyList(
                standby = PreviewStandby,
                enabled = true,
                onDraggingChanged = {},
                onReorder = {}
            )
        }
    }
}

@Preview
@Composable
private fun StandbyPositionDiscPreview() {
    MusterTheme {
        Surface(color = MusterColors.QuietSurface) {
            StandbyPositionDisc(position = 1, modifier = Modifier.padding(8.dp))
        }
    }
}

@Preview
@Composable
private fun StandbyRowItemPreview() {
    MusterTheme {
        Surface(color = MusterColors.QuietSurface) {
            StandbyRowItem(position = 2, row = PreviewStandby[1])
        }
    }
}

@Preview
@Composable
private fun StandbySectionAdminPreview() {
    MusterTheme {
        Surface {
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                standbySection(
                    standby = PreviewStandby,
                    isAdmin = true,
                    phase = EventPhase.Active,
                    reordering = false,
                    error = null,
                    onDraggingChanged = {},
                    onReorder = {}
                )
            }
        }
    }
}

@Preview
@Composable
private fun StandbySectionMemberPreview() {
    MusterTheme {
        Surface {
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                standbySection(
                    standby = PreviewStandby,
                    isAdmin = false,
                    phase = EventPhase.Active,
                    reordering = false,
                    error = null,
                    onDraggingChanged = {},
                    onReorder = {}
                )
            }
        }
    }
}

@Preview
@Composable
private fun StandbySectionLockedPreview() {
    MusterTheme {
        Surface {
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                standbySection(
                    standby = PreviewStandby,
                    isAdmin = true,
                    phase = EventPhase.Locked(byName = "Ali"),
                    reordering = false,
                    error = null,
                    onDraggingChanged = {},
                    onReorder = {}
                )
            }
        }
    }
}

@Preview
@Composable
private fun StandbySectionStartedPreview() {
    MusterTheme {
        Surface {
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                standbySection(
                    standby = PreviewStandby,
                    isAdmin = true,
                    phase = EventPhase.Started(atDisplay = "7:00 pm"),
                    reordering = false,
                    error = null,
                    onDraggingChanged = {},
                    onReorder = {}
                )
            }
        }
    }
}

@Preview
@Composable
private fun StandbySectionErrorPreview() {
    MusterTheme {
        Surface {
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                standbySection(
                    standby = PreviewStandby,
                    isAdmin = true,
                    phase = EventPhase.Active,
                    reordering = false,
                    error = DomainError.EventFull(),
                    onDraggingChanged = {},
                    onReorder = {}
                )
            }
        }
    }
}
