package app.muster.ui.screens.group.members

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.muster.domain.error.DomainError
import app.muster.ui.common.components.MessageState
import app.muster.ui.common.components.MusterPullToRefreshBox
import app.muster.ui.common.components.MusterSpinner
import app.muster.ui.common.components.TonalPill
import app.muster.ui.common.toMessage
import app.muster.ui.screens.group.members.components.MemberOperations
import app.muster.ui.screens.group.members.components.MemberRowItem
import app.muster.ui.screens.group.members.components.MemberTagCardActions
import app.muster.ui.screens.group.members.components.MemberTagCardEmptyHint
import app.muster.ui.screens.group.members.components.MemberTagCardHeader
import app.muster.ui.screens.group.members.components.MemberUntaggedDropHint
import app.muster.ui.screens.group.members.components.TagCardPosition
import app.muster.ui.screens.group.members.components.TagCardRowPadding
import app.muster.ui.screens.group.members.components.TagsIntroPanel
import app.muster.ui.screens.group.members.components.rememberMemberRowActionsWithDialogs
import app.muster.ui.screens.group.members.components.rememberTagDeleteAction
import app.muster.ui.screens.group.members.components.tagCardFrame
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import kotlinx.coroutines.delay
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.members_add_by_email
import muster.shared.generated.resources.members_count
import muster.shared.generated.resources.members_empty_body
import muster.shared.generated.resources.members_empty_title
import muster.shared.generated.resources.members_failed_title
import muster.shared.generated.resources.members_new_tag
import muster.shared.generated.resources.members_pending_suffix
import muster.shared.generated.resources.members_section_invited
import muster.shared.generated.resources.members_section_untagged
import muster.shared.generated.resources.members_section_untagged_sort
import muster.shared.generated.resources.members_try_again
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState

data class MembersTabActions(
    val onAddMemberByEmail: () -> Unit,
    val onRetry: () -> Unit,
    val onRefresh: () -> Unit = {},
    val memberOperations: MemberOperations,
    val onCreateTag: () -> Unit,
    val onDeleteTag: (String) -> Unit,
    val onMoveUpTag: (String) -> Unit,
    val onMoveDownTag: (String) -> Unit,
    val onRenameTag: (String, String, Int) -> Unit
)

@Composable
fun MembersTabRoute(
    groupId: String,
    onAddMemberByEmail: () -> Unit,
    onCreateTagForMember: (String) -> Unit,
    onCreateTag: () -> Unit,
    onRenameMemberTag: (String, String, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MembersTabViewModel = koinViewModel { parametersOf(groupId) }
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    val state by viewModel.state.collectAsStateWithLifecycle()
    MembersTabScreen(
        state = state,
        actions = MembersTabActions(
            onAddMemberByEmail = onAddMemberByEmail,
            onRetry = viewModel::onRetry,
            onRefresh = viewModel::onRefresh,
            memberOperations = MemberOperations(
                onPromote = viewModel::onPromote,
                onDemote = viewModel::onDemote,
                onRemove = viewModel::onRemove,
                onRevokeInvitation = viewModel::onRevokeInvitation,
                onResendInvitation = viewModel::onResendInvitation,
                onSetMemberTag = viewModel::onSetMemberTag,
                onCreateTagForMember = onCreateTagForMember
            ),
            onCreateTag = onCreateTag,
            onDeleteTag = viewModel::onDeleteTag,
            onMoveUpTag = viewModel::onMoveTagUp,
            onMoveDownTag = viewModel::onMoveTagDown,
            onRenameTag = onRenameMemberTag
        ),
        actionErrorMessage = (state as? MembersTabUiState.Success)?.actionError?.toMessage(),
        modifier = modifier
    )
}

@Composable
fun MembersTabScreen(
    state: MembersTabUiState,
    actions: MembersTabActions,
    modifier: Modifier = Modifier,
    actionErrorMessage: String? = null,
    spinnerDelayMillis: Long = 400
) {
    when (state) {
        is MembersTabUiState.Loading -> ShowLoading(spinnerDelayMillis = spinnerDelayMillis, modifier = modifier)

        is MembersTabUiState.Error -> ShowError(state = state, actions = actions, modifier = modifier)

        is MembersTabUiState.Success -> MembersContent(
            state = state,
            actions = actions,
            actionErrorMessage = actionErrorMessage,
            modifier = modifier
        )
    }
}

@Composable
private fun ShowLoading(
    spinnerDelayMillis: Long,
    modifier: Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        MusterSpinner(delayMillis = spinnerDelayMillis)
    }
}

@Composable
private fun ShowError(
    state: MembersTabUiState.Error,
    actions: MembersTabActions,
    modifier: Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        MessageState(
            title = stringResource(Res.string.members_failed_title),
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
                    text = stringResource(Res.string.members_try_again),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun MembersContent(
    state: MembersTabUiState.Success,
    actions: MembersTabActions,
    actionErrorMessage: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        MembersHeader(state = state, actions = actions)
        MusterPullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = actions.onRefresh,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            if (state.isEmpty) {
                MembersEmptyState()
            } else {
                MembersList(
                    state = state,
                    actions = actions,
                    actionErrorMessage = actionErrorMessage
                )
            }
        }
    }
}

@Composable
private fun MembersHeader(
    state: MembersTabUiState.Success,
    actions: MembersTabActions
) {
    MemberCountLine(
        memberCount = state.memberCount,
        pendingCount = state.pendingCount,
        onNewTag = { actions.onCreateTag() }.takeIf { state.showNewTagButton }
    )
    state.moveError?.let {
        Text(
            text = it.toMessage(),
            style = MaterialTheme.typography.bodySmall,
            color = MusterColors.OutText,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
        )
    }
    if (state.canAddMembers) {
        AddMemberByEmailRow(onClick = actions.onAddMemberByEmail)
        HorizontalDivider(color = MusterColors.Hairline)
    }
}

@Composable
private fun MembersEmptyState() {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                MessageState(
                    title = stringResource(Res.string.members_empty_title),
                    body = stringResource(Res.string.members_empty_body)
                )
            }
        }
    }
}

@Composable
private fun MembersList(
    state: MembersTabUiState.Success,
    actions: MembersTabActions,
    actionErrorMessage: String?
) {
    val requestMemberRowActions = rememberMemberRowActionsWithDialogs(
        memberOperations = actions.memberOperations,
        tagsOptions = state.tagOptions
    )
    val requestTagDelete = rememberTagDeleteAction(actions.onDeleteTag)

    val latestState by rememberUpdatedState(state)
    val latestActions by rememberUpdatedState(actions)
    var dragLayout by remember { mutableStateOf<DragLayout?>(null) }
    val stateEntries = remember(state.tags, state.members, state.pendings) { buildEntries(state) }
    val entries = dragLayout?.entries ?: stateEntries

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

    val rowItem: @Composable (MemberRow, Modifier, PaddingValues) -> Unit = { row, dragHandleModifier, contentPadding ->
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

    val drag = MemberDragCallbacks(
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

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        introPanelSection(state, { actions.onCreateTag() })
        entriesSection(
            entries = entries,
            state = state,
            actions = actions,
            requestTagDelete = requestTagDelete,
            reorderableState = reorderableState,
            drag = drag,
            rowItem = rowItem,
            errorFor = { tagId -> actionErrorMessage.takeIf { state.failedActionId == tagId } }
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
    rowItem: @Composable (MemberRow, Modifier, PaddingValues) -> Unit,
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
private fun MemberCountLine(
    memberCount: Int,
    pendingCount: Int,
    onNewTag: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = buildAnnotatedString {
                append(pluralStringResource(Res.plurals.members_count, memberCount, memberCount))
                if (pendingCount > 0) {
                    withStyle(SpanStyle(color = MusterColors.Muted, fontWeight = FontWeight.Normal)) {
                        append(stringResource(Res.string.members_pending_suffix, pendingCount))
                    }
                }
            },
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MusterColors.Secondary,
            modifier = Modifier.weight(1f)
        )
        if (onNewTag != null) {
            TonalPill(text = stringResource(Res.string.members_new_tag), onClick = onNewTag)
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

@Composable
private fun AddMemberByEmailRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(percent = 50),
            border = BorderStroke(1.dp, MusterColors.Outline),
            color = MusterColors.White
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "+", fontSize = 20.sp, color = MusterColors.Ink)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(Res.string.members_add_by_email),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

private val PreviewActions = MembersTabActions(
    onAddMemberByEmail = {},
    onRetry = {},
    memberOperations = MemberOperations(),
    onCreateTag = {},
    onDeleteTag = {},
    onMoveUpTag = {},
    onMoveDownTag = {},
) { _, _, _ -> }

private val AdminPreviewMembers = listOf(
    MemberRow(id = "1", displayName = "Alex Doyle", status = MemberStatus.Admin, isSelf = true),
    MemberRow(id = "2", displayName = "Dan Whelan", status = MemberStatus.Admin, canDemote = true, canRemove = true),
    MemberRow(id = "3", displayName = "Marcus Keane", status = MemberStatus.Member, canPromote = true, canRemove = true),
    MemberRow(id = "4", displayName = "Sam Okafor", status = MemberStatus.Member, canPromote = true, canRemove = true),
    MemberRow(id = "5", displayName = "Tomás Neale", status = MemberStatus.Member, canPromote = true, canRemove = true)
)

private val AdminPreviewPendings = listOf(
    MemberRow(
        id = "6",
        displayName = "j.moriarty@outlook.com",
        status = MemberStatus.Pending,
        canRevokeInvitation = true,
        canResendInvitation = true
    ),
    MemberRow(
        id = "7",
        displayName = "priya.n@gmail.com",
        status = MemberStatus.Pending,
        canRevokeInvitation = true,
        canResendInvitation = true
    )
)

private val MemberPreviewRows = listOf(
    MemberRow(id = "1", displayName = "Alex Doyle", status = MemberStatus.Admin),
    MemberRow(id = "2", displayName = "Dan Whelan", status = MemberStatus.Admin),
    MemberRow(id = "3", displayName = "Marcus Keane", status = MemberStatus.Member, isSelf = true),
    MemberRow(id = "4", displayName = "Sam Okafor", status = MemberStatus.Member),
    MemberRow(id = "5", displayName = "Tomás Neale", status = MemberStatus.Member)
)

@Preview
@Composable
private fun MembersTabScreenAdminPreview() {
    MusterTheme {
        Surface {
            MembersTabScreen(
                state = MembersTabUiState.Success(members = AdminPreviewMembers, pendings = AdminPreviewPendings, canAddMembers = true),
                actions = PreviewActions
            )
        }
    }
}

// 5m
@Preview
@Composable
private fun MembersTabScreenAdminNoTagsPreview() {
    MusterTheme {
        Surface {
            MembersTabScreen(
                state = MembersTabUiState.Success(
                    members = AdminPreviewMembers,
                    pendings = AdminPreviewPendings,
                    canAddMembers = true,
                    canManageTags = true
                ),
                actions = PreviewActions
            )
        }
    }
}

private fun previewTagged(
    id: String,
    name: String,
    status: MemberStatus,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    isSelf: Boolean = false
) = MemberRow(
    id = id,
    displayName = name,
    status = status,
    isSelf = isSelf,
    canMoveToTag = true,
    canPromote = !isSelf && status == MemberStatus.Member,
    canDemote = !isSelf && status == MemberStatus.Admin,
    canRemove = !isSelf
)

private val PreviewTags = listOf(
    TagSection(
        id = "t1",
        name = "Regular",
        rows = listOf(
            previewTagged("1", "Alex Doyle", MemberStatus.Admin, canMoveUp = false, canMoveDown = true, isSelf = true),
            previewTagged("2", "Dan Whelan", MemberStatus.Admin, canMoveUp = true, canMoveDown = true),
            previewTagged("3", "Marcus Keane", MemberStatus.Member, canMoveUp = true, canMoveDown = false)
        ),
        reorder = Reorder(canMoveUp = false, canMoveDown = true)
    ),
    TagSection(
        id = "t2",
        name = "Casual",
        rows = listOf(previewTagged("4", "Leo Fanning", MemberStatus.Member, canMoveUp = false, canMoveDown = false)),
        reorder = Reorder(canMoveUp = true, canMoveDown = true)
    ),
    TagSection(id = "t3", name = "Juniors", rows = emptyList(), reorder = Reorder(canMoveUp = true, canMoveDown = false))
)

private val PreviewUntagged = listOf(
    MemberRow(id = "5", displayName = "Conor Blake", status = MemberStatus.Member, canMoveToTag = true, canPromote = true, canRemove = true),
    MemberRow(id = "6", displayName = "Priya Nair", status = MemberStatus.Member, canMoveToTag = true, canPromote = true, canRemove = true)
)

private val PreviewInvited = listOf(
    MemberRow(
        id = "7",
        displayName = "kate.byrne@outlook.com",
        status = MemberStatus.Pending,
        canRevokeInvitation = true,
        canResendInvitation = true
    )
)

// 5a, 5b
@Preview(heightDp = 1400)
@Composable
private fun MembersTabScreenAdminWithTagsPreview() {
    MusterTheme {
        Surface {
            MembersTabScreen(
                state = MembersTabUiState.Success(
                    tags = PreviewTags,
                    members = PreviewUntagged,
                    pendings = PreviewInvited,
                    canAddMembers = true,
                    canManageTags = true
                ),
                actions = PreviewActions
            )
        }
    }
}

@Preview(heightDp = 1400)
@Composable
private fun MembersTabScreenMovingPreview() {
    MusterTheme {
        Surface {
            MembersTabScreen(
                state = MembersTabUiState.Success(
                    tags = PreviewTags,
                    members = PreviewUntagged,
                    pendings = PreviewInvited,
                    canAddMembers = true,
                    canManageTags = true,
                    movingProfileId = "4",
                    actionTargetId = "4"
                ),
                actions = PreviewActions
            )
        }
    }
}

@Preview(heightDp = 1400)
@Composable
private fun MembersTabScreenTagErrorsPreview() {
    MusterTheme {
        Surface {
            MembersTabScreen(
                state = MembersTabUiState.Success(
                    tags = PreviewTags,
                    members = PreviewUntagged,
                    pendings = PreviewInvited,
                    canAddMembers = true,
                    canManageTags = true,
                    actionError = DomainError.Network(),
                    failedActionId = "t1",
                    moveError = DomainError.Network()
                ),
                actions = PreviewActions
            )
        }
    }
}

@Preview
@Composable
private fun MembersTabScreenMemberPreview() {
    MusterTheme {
        Surface {
            MembersTabScreen(
                state = MembersTabUiState.Success(members = MemberPreviewRows, canAddMembers = false),
                actions = PreviewActions
            )
        }
    }
}

@Preview
@Composable
private fun MembersTabScreenEmptyPreview() {
    MusterTheme {
        Surface {
            MembersTabScreen(
                state = MembersTabUiState.Success(
                    members = listOf(MemberRow(id = "1", displayName = "Alex Doyle", status = MemberStatus.Admin, isSelf = true)),
                    canAddMembers = true
                ),
                actions = PreviewActions
            )
        }
    }
}

@Preview
@Composable
private fun MembersTabScreenLoadingPreview() {
    MusterTheme {
        Surface {
            MembersTabScreen(
                state = MembersTabUiState.Loading,
                actions = PreviewActions,
                spinnerDelayMillis = 0
            )
        }
    }
}

@Preview
@Composable
private fun MembersTabScreenFailedPreview() {
    MusterTheme {
        Surface {
            MembersTabScreen(
                state = MembersTabUiState.Error(DomainError.Network()),
                actions = PreviewActions
            )
        }
    }
}
