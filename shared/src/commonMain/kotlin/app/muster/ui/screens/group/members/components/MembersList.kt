package app.muster.ui.screens.group.members.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.muster.ui.screens.group.members.MemberRow
import app.muster.ui.screens.group.members.MemberStatus
import app.muster.ui.screens.group.members.MembersTabActions
import app.muster.ui.screens.group.members.MembersTabUiState
import app.muster.ui.screens.group.members.TagSection
import app.muster.ui.theme.MusterColors
import kotlinx.coroutines.delay
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.members_section_invited
import muster.shared.generated.resources.members_section_untagged
import muster.shared.generated.resources.members_section_untagged_sort
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState

private typealias MemberRowContent = @Composable (MemberRow, Modifier, PaddingValues) -> Unit

@Composable
internal fun MembersList(
    state: MembersTabUiState.Success,
    actions: MembersTabActions,
    actionErrorMessage: String?
) {
    val drag = rememberMembersDragState(state, actions)
    val rowItem = memberRowRenderer(state, actions, actionErrorMessage)
    val requestTagDelete = rememberTagDeleteAction(actions.onDeleteTag)

    LazyColumn(
        state = drag.lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        introPanelSection(state, { actions.onCreateTag() })
        entriesSection(
            entries = drag.entries,
            state = state,
            actions = actions,
            requestTagDelete = requestTagDelete,
            reorderableState = drag.reorderableState,
            drag = drag.callbacks,
            rowItem = rowItem,
            errorFor = { tagId -> actionErrorMessage.takeIf { state.failedActionId == tagId } }
        )
    }
}

private class MembersDragState(
    val entries: List<MembersEntry>,
    val lazyListState: LazyListState,
    val reorderableState: ReorderableLazyListState,
    val callbacks: MemberDragCallbacks
)

@Composable
private fun rememberMembersDragState(
    state: MembersTabUiState.Success,
    actions: MembersTabActions
): MembersDragState {
    val latestState by rememberUpdatedState(state)
    val latestActions by rememberUpdatedState(actions)
    var dragLayout by remember { mutableStateOf<DragLayout?>(null) }
    val stateEntries = remember(state.tags, state.members, state.pendings) { buildEntries(state) }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        dragLayout = dragLayout?.moved(from.key, to.key)
    }

    LaunchedEffect(dragLayout?.settling) {
        if (dragLayout?.settling == true) {
            delay(DragSettleMillis)
            dragLayout = null
        }
    }

    val callbacks = MemberDragCallbacks(
        enabled = state.actionTargetId == null && dragLayout?.settling != true,
        onStart = { rowId ->
            val current = buildEntries(latestState, dragging = true)
            dragLayout = DragLayout(entries = current, draggedId = rowId, origin = current.sectionOf(rowId))
        },
        onStop = {
            dragLayout?.let { layout ->
                val destination = layout.destination()
                if (destination != layout.origin && destination !is ListSection.Invited) {
                    latestActions.memberOperations.onSetMemberTag(layout.draggedId, (destination as? ListSection.Tag)?.id)
                }
                dragLayout = layout.copy(settling = true)
            }
        }
    )
    return MembersDragState(
        entries = dragLayout?.entries ?: stateEntries,
        lazyListState = lazyListState,
        reorderableState = reorderableState,
        callbacks = callbacks
    )
}

@Composable
private fun memberRowRenderer(
    state: MembersTabUiState.Success,
    actions: MembersTabActions,
    actionErrorMessage: String?
): MemberRowContent {
    val requestMemberRowActions = rememberMemberRowActionsWithDialogs(
        memberOperations = actions.memberOperations,
        tagsOptions = state.tagOptions
    )
    return { row, dragHandleModifier, contentPadding ->
        val inFlight = row.inFlight(state.actionTargetId)
        val errorMessage = actionErrorMessage.takeIf { state.failedActionId == row.id }
        val isPending = row.status == MemberStatus.Pending
        MemberRowItem(
            row = row,
            memberRowActions = requestMemberRowActions(row),
            inFlight = inFlight,
            errorMessage = errorMessage,
            isPending = isPending,
            isMoving = state.movingProfileId == row.id,
            dragHandleModifier = dragHandleModifier,
            contentPadding = contentPadding
        )
    }
}

private fun LazyListScope.introPanelSection(
    state: MembersTabUiState.Success,
    onNewTag: () -> Unit
) {
    if (state.showTagsIntro) {
        item(key = "tags_intro") {
            TagsIntroPanel(
                onNewTag = onNewTag,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

private val MemberRowPadding = PaddingValues(horizontal = 20.dp)

private class MemberDragCallbacks(
    val enabled: Boolean,
    val onStart: (String) -> Unit,
    val onStop: () -> Unit
)

private fun LazyListScope.entriesSection(
    entries: List<MembersEntry>,
    state: MembersTabUiState.Success,
    actions: MembersTabActions,
    requestTagDelete: (TagSection) -> Unit,
    reorderableState: ReorderableLazyListState,
    drag: MemberDragCallbacks,
    rowItem: MemberRowContent,
    errorFor: (String) -> String?
) {
    var currentSection: ListSection? = null
    entries.forEachIndexed { index, entry ->
        when (entry) {
            is HeaderEntry -> {
                currentSection = entry.section
                val rows = entries.rowsAfter(index)
                when (val section = entry.section) {
                    is ListSection.Tag -> tagHeaderItems(section.id, rows, state, actions, requestTagDelete, errorFor)

                    ListSection.Untagged -> item(key = entry.key) {
                        SectionHeader(
                            title = stringResource(Res.string.members_section_untagged),
                            count = rows.size,
                            trailing = stringResource(Res.string.members_section_untagged_sort),
                            modifier = Modifier.animateItem()
                        )
                    }

                    ListSection.Invited -> item(key = entry.key) {
                        SectionHeader(
                            title = stringResource(Res.string.members_section_invited),
                            count = rows.size,
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }

            is HintEntry -> item(key = entry.key) {
                ReorderableItem(reorderableState, key = entry.key, modifier = Modifier.fillMaxWidth()) {
                    when (entry.section) {
                        is ListSection.Tag -> MemberTagCardEmptyHint(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp))
                        else -> MemberUntaggedDropHint(Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
                    }
                }
            }

            is RowEntry -> {
                val row = entry.row
                val inTag = currentSection is ListSection.Tag
                val isLast = entries.getOrNull(index + 1) !is RowEntry
                val reorderable = row.canMoveToTag && row.status != MemberStatus.Pending
                item(key = entry.key) {
                    val content: @Composable (Modifier) -> Unit = { handle ->
                        RowSlot(inTag = inTag, isLast = isLast) {
                            rowItem(row, handle, if (inTag) TagCardRowPadding else MemberRowPadding)
                        }
                    }
                    if (reorderable) {
                        ReorderableItem(reorderableState, key = row.id, modifier = Modifier.fillMaxWidth()) {
                            content(
                                Modifier.draggableHandle(
                                    enabled = drag.enabled,
                                    onDragStarted = { drag.onStart(row.id) },
                                    onDragStopped = drag.onStop
                                )
                            )
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().animateItem()) { content(Modifier) }
                    }
                }
            }
        }
    }
}

// One call chain for tagged and untagged rows: a dragged row changes between them mid-drag, and a
// different structure would recreate the row and cancel its drag.
@Composable
private fun RowSlot(
    inTag: Boolean,
    isLast: Boolean,
    content: @Composable () -> Unit
) {
    val frame = if (inTag) {
        Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = if (isLast) 6.dp else 0.dp)
            .tagCardFrame(if (isLast) TagCardPosition.Last else TagCardPosition.Middle)
    } else {
        Modifier
    }
    Column(modifier = Modifier.fillMaxWidth().then(frame)) {
        if (inTag) HorizontalDivider(color = MusterColors.Hairline)
        content()
    }
}

private fun LazyListScope.tagHeaderItems(
    tagId: String,
    rows: List<MemberRow>,
    state: MembersTabUiState.Success,
    actions: MembersTabActions,
    requestTagDelete: (TagSection) -> Unit,
    errorFor: (String) -> String?
) {
    item(key = "tag_header_$tagId") {
        state.tags.firstOrNull { it.id == tagId }?.let { section ->
            MemberTagCardHeader(
                section = section.copy(rows = rows),
                actions = MemberTagCardActions(
                    onDelete = { requestTagDelete(section) },
                    onMoveUp = { actions.onMoveUpTag(tagId) },
                    onMoveDown = { actions.onMoveDownTag(tagId) },
                    onRename = { actions.onRenameTag(tagId, section.name, rows.size) }
                ),
                modifier = Modifier.animateItem().padding(start = 16.dp, end = 16.dp, top = 6.dp),
                errorMessage = errorFor(tagId)
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
    trailing: String? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MusterColors.Ink
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MusterColors.Muted,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Text(text = trailing, style = MaterialTheme.typography.labelSmall, color = MusterColors.Hint)
        }
    }
}
