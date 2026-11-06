package app.muster.ui.screens.event.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.muster.ui.common.components.MusterIcons
import app.muster.ui.common.util.SharedTransitionKeys
import app.muster.ui.common.util.sharedBoundsOrNone
import app.muster.ui.screens.event.EventOverflowAction
import app.muster.ui.screens.event.EventSummary
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.content_description_back
import muster.shared.generated.resources.content_description_more
import muster.shared.generated.resources.event_menu_cancel
import muster.shared.generated.resources.event_menu_delete
import muster.shared.generated.resources.event_menu_edit
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun EventAppBar(
    summary: EventSummary,
    overflowActions: List<EventOverflowAction>,
    onBack: () -> Unit,
    onOverflowAction: (EventOverflowAction) -> Unit,
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
                modifier = Modifier.sharedBoundsOrNone(SharedTransitionKeys.groupName(summary.groupId)).padding(top = 8.dp)
            )
            Text(
                text = summary.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.sharedBoundsOrNone(SharedTransitionKeys.eventTitle(summary.eventId)).padding(bottom = 8.dp)
            )
        }
        if (overflowActions.isNotEmpty()) {
            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        imageVector = MusterIcons.MoreVert,
                        contentDescription = stringResource(Res.string.content_description_more),
                        tint = MusterColors.Ink,
                        modifier = Modifier.size(24.dp)
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    overflowActions.forEach { action ->
                        DropdownMenuItem(
                            text = { Text(stringResource(action.menuLabel())) },
                            onClick = {
                                expanded = false
                                onOverflowAction(action)
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun EventOverflowAction.menuLabel() = when (this) {
    EventOverflowAction.Edit -> Res.string.event_menu_edit
    EventOverflowAction.Cancel -> Res.string.event_menu_cancel
    EventOverflowAction.Delete -> Res.string.event_menu_delete
}

@Preview
@Composable
private fun EventAppBarPreview() {
    MusterTheme {
        Surface {
            EventAppBar(
                summary = PreviewSummary,
                overflowActions = EventOverflowAction.entries,
                onBack = {},
                onOverflowAction = {}
            )
        }
    }
}

@Preview
@Composable
private fun EventAppBarNoMenuPreview() {
    MusterTheme {
        Surface {
            EventAppBar(
                summary = PreviewSummary,
                overflowActions = emptyList(),
                onBack = {},
                onOverflowAction = {}
            )
        }
    }
}
