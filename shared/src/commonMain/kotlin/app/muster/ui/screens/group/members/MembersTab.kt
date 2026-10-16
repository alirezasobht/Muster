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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.muster.domain.error.DomainError
import app.muster.ui.common.components.ConfirmDialog
import app.muster.ui.common.components.MessageState
import app.muster.ui.common.components.MusterPullToRefreshBox
import app.muster.ui.common.components.MusterSpinner
import app.muster.ui.common.components.TonalPill
import app.muster.ui.common.toMessage
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.members_add_by_email
import muster.shared.generated.resources.members_count
import muster.shared.generated.resources.members_demote_body
import muster.shared.generated.resources.members_demote_cancel
import muster.shared.generated.resources.members_demote_confirm
import muster.shared.generated.resources.members_demote_title
import muster.shared.generated.resources.members_empty_body
import muster.shared.generated.resources.members_empty_title
import muster.shared.generated.resources.members_failed_title
import muster.shared.generated.resources.members_new_tag
import muster.shared.generated.resources.members_pending_suffix
import muster.shared.generated.resources.members_promote_body
import muster.shared.generated.resources.members_promote_cancel
import muster.shared.generated.resources.members_promote_confirm
import muster.shared.generated.resources.members_promote_title
import muster.shared.generated.resources.members_remove_body
import muster.shared.generated.resources.members_remove_cancel
import muster.shared.generated.resources.members_remove_confirm
import muster.shared.generated.resources.members_remove_title
import muster.shared.generated.resources.members_resend_body
import muster.shared.generated.resources.members_resend_cancel
import muster.shared.generated.resources.members_resend_confirm
import muster.shared.generated.resources.members_resend_title
import muster.shared.generated.resources.members_revoke_body
import muster.shared.generated.resources.members_revoke_cancel
import muster.shared.generated.resources.members_revoke_confirm
import muster.shared.generated.resources.members_revoke_title
import muster.shared.generated.resources.members_section_invited
import muster.shared.generated.resources.members_section_untagged
import muster.shared.generated.resources.members_section_untagged_sort
import muster.shared.generated.resources.members_try_again
import muster.shared.generated.resources.tag_delete_body
import muster.shared.generated.resources.tag_delete_body_empty
import muster.shared.generated.resources.tag_delete_cancel
import muster.shared.generated.resources.tag_delete_confirm
import muster.shared.generated.resources.tag_delete_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

data class MembersActions(
    val onAddMemberByEmail: () -> Unit,
    val onRetry: () -> Unit,
    val onRefresh: () -> Unit = {},
    val onNewTag: () -> Unit = {},
    val memberRowActions: MemberRowActions,
    val tagCardActions: TagCardActions = TagCardActions()
)

@Composable
fun MembersRoute(
    groupId: String,
    onAddMemberByEmail: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MembersViewModel = koinViewModel { parametersOf(groupId) }
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    val state by viewModel.state.collectAsStateWithLifecycle()
    MembersTab(
        state = state,
        actions = MembersActions(
            onAddMemberByEmail = onAddMemberByEmail,
            onRetry = viewModel::onRetry,
            onRefresh = viewModel::onRefresh,
            memberRowActions = MemberRowActions(
                onPromote = viewModel::onPromote,
                onDemote = viewModel::onDemote,
                onRemove = viewModel::onRemove,
                onRevokeInvitation = viewModel::onRevokeInvitation,
                onResendInvitation = viewModel::onResendInvitation
            )
        ),
        actionErrorMessage = (state as? MembersUiState.Success)?.actionError?.toMessage(),
        modifier = modifier
    )
}

@Composable
fun MembersTab(
    state: MembersUiState,
    actions: MembersActions,
    modifier: Modifier = Modifier,
    actionErrorMessage: String? = null,
    spinnerDelayMillis: Long = 400
) {
    when (state) {
        is MembersUiState.Loading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            MusterSpinner(delayMillis = spinnerDelayMillis)
        }
        is MembersUiState.Error -> Box(
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
        is MembersUiState.Success -> MembersContent(
            state = state,
            actions = actions,
            actionErrorMessage = actionErrorMessage,
            modifier = modifier
        )
    }
}

private data class DialogStrings(
    val title: StringResource,
    val body: StringResource,
    val confirm: StringResource,
    val cancel: StringResource
)

private val PromoteDialog = DialogStrings(
    Res.string.members_promote_title,
    Res.string.members_promote_body,
    Res.string.members_promote_confirm,
    Res.string.members_promote_cancel
)
private val DemoteDialog = DialogStrings(
    Res.string.members_demote_title,
    Res.string.members_demote_body,
    Res.string.members_demote_confirm,
    Res.string.members_demote_cancel
)
private val RemoveDialog = DialogStrings(
    Res.string.members_remove_title,
    Res.string.members_remove_body,
    Res.string.members_remove_confirm,
    Res.string.members_remove_cancel
)
private val RevokeDialog = DialogStrings(
    Res.string.members_revoke_title,
    Res.string.members_revoke_body,
    Res.string.members_revoke_confirm,
    Res.string.members_revoke_cancel
)
private val ResendDialog = DialogStrings(
    Res.string.members_resend_title,
    Res.string.members_resend_body,
    Res.string.members_resend_confirm,
    Res.string.members_resend_cancel
)

// The dialog carries the call to make on confirm, not an action tag to map
// back to one afterwards.
private data class PendingConfirmation(
    val row: MemberRow,
    val dialog: DialogStrings,
    val onConfirm: () -> Unit
)

@Composable
private fun MembersContent(
    state: MembersUiState.Success,
    actions: MembersActions,
    actionErrorMessage: String?,
    modifier: Modifier = Modifier
) {
    var pendingConfirmation by remember { mutableStateOf<PendingConfirmation?>(null) }
    var pendingTagDelete by remember { mutableStateOf<TagSection?>(null) }

    // The role and invitation items ask for confirmation first; the tag items go straight through.
    fun rowActions(row: MemberRow): MemberRowActions {
        val passed = actions.memberRowActions
        fun confirm(
            dialog: DialogStrings,
            call: (String) -> Unit
        ): (String) -> Unit = { id ->
            pendingConfirmation = PendingConfirmation(row, dialog) { call(id) }
        }
        return passed.copy(
            onPromote = confirm(PromoteDialog, passed.onPromote),
            onDemote = confirm(DemoteDialog, passed.onDemote),
            onRemove = confirm(RemoveDialog, passed.onRemove),
            onRevokeInvitation = confirm(RevokeDialog, passed.onRevokeInvitation),
            onResendInvitation = confirm(ResendDialog, passed.onResendInvitation)
        )
    }

    @Composable
    fun MemberItem(
        row: MemberRow,
        modifier: Modifier = Modifier,
        horizontalPadding: Dp = 20.dp
    ) {
        MemberRowItem(
            row = row,
            actions = rowActions(row),
            inFlight = state.actionTargetId == row.id,
            errorMessage = actionErrorMessage.takeIf { state.failedActionId == row.id },
            horizontalPadding = horizontalPadding,
            modifier = modifier
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        MemberCountLine(
            memberCount = state.memberCount,
            pendingCount = state.pendingCount,
            onNewTag = actions.onNewTag.takeIf { state.showNewTagButton }
        )
        if (state.canAddMembers) {
            AddMemberByEmailRow(onClick = actions.onAddMemberByEmail)
            HorizontalDivider(color = MusterColors.Hairline)
        }
        MusterPullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = actions.onRefresh,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            if (state.isEmpty) {
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
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    if (state.showTagsIntro) {
                        item(key = "tags_intro") {
                            TagsIntroPanel(
                                onNewTag = actions.onNewTag,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                    }
                    if (state.tags.isEmpty()) {
                        items(state.rows, key = { it.id }) { MemberItem(it, Modifier.animateItem()) }
                    } else {
                        // 5a/5b: tag cards in the admin's order, then Untagged A–Z, then Invited.
                        items(state.tags, key = { "tag_${it.id}" }) { section ->
                            TagCard(
                                section = section,
                                actions = actions.tagCardActions.copy(
                                    onDelete = { id -> pendingTagDelete = state.tags.firstOrNull { it.id == id } }
                                ),
                                modifier = Modifier.animateItem().padding(horizontal = 16.dp, vertical = 6.dp)
                            ) { MemberItem(it, horizontalPadding = 16.dp) }
                        }
                        val untagged = state.untaggedRows
                        if (untagged.isNotEmpty()) {
                            item(key = "untagged_header") {
                                SectionHeader(
                                    title = stringResource(Res.string.members_section_untagged),
                                    count = untagged.size,
                                    trailing = stringResource(Res.string.members_section_untagged_sort)
                                )
                            }
                            items(untagged, key = { it.id }) { MemberItem(it, Modifier.animateItem()) }
                        }
                        val invited = state.invitedRows
                        if (invited.isNotEmpty()) {
                            item(key = "invited_header") {
                                SectionHeader(title = stringResource(Res.string.members_section_invited), count = invited.size)
                            }
                            items(invited, key = { it.id }) { MemberItem(it, Modifier.animateItem()) }
                        }
                    }
                }
            }
        }
    }

    // 5s: even an empty tag asks first
    pendingTagDelete?.let { tag ->
        ConfirmDialog(
            title = stringResource(Res.string.tag_delete_title, tag.name),
            // English has no "zero" plural, so an empty tag can't use the plural's zero form.
            body = if (tag.rows.isEmpty()) {
                stringResource(Res.string.tag_delete_body_empty)
            } else {
                pluralStringResource(Res.plurals.tag_delete_body, tag.rows.size, tag.rows.size)
            },
            confirmText = stringResource(Res.string.tag_delete_confirm),
            cancelText = stringResource(Res.string.tag_delete_cancel),
            onConfirm = {
                pendingTagDelete = null
                actions.tagCardActions.onDelete(tag.id)
            },
            onDismiss = { pendingTagDelete = null },
            destructive = true
        )
    }

    pendingConfirmation?.let { pending ->
        ConfirmDialog(
            title = stringResource(pending.dialog.title, pending.row.displayName),
            body = stringResource(pending.dialog.body),
            confirmText = stringResource(pending.dialog.confirm),
            cancelText = stringResource(pending.dialog.cancel),
            onConfirm = {
                pendingConfirmation = null
                pending.onConfirm()
            },
            onDismiss = { pendingConfirmation = null }
        )
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

private val PreviewActions = MembersActions(
    onAddMemberByEmail = {},
    memberRowActions = MemberRowActions(),
    onRetry = {}
)

private val AdminPreviewRows = listOf(
    MemberRow(id = "1", displayName = "Alex Doyle", status = MemberStatus.Admin, isSelf = true),
    MemberRow(id = "2", displayName = "Dan Whelan", status = MemberStatus.Admin, canDemote = true, canRemove = true),
    MemberRow(id = "3", displayName = "Marcus Keane", status = MemberStatus.Member, canPromote = true, canRemove = true),
    MemberRow(id = "4", displayName = "Sam Okafor", status = MemberStatus.Member, canPromote = true, canRemove = true),
    MemberRow(id = "5", displayName = "Tomás Neale", status = MemberStatus.Member, canPromote = true, canRemove = true),
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
private fun MembersTabAdminPreview() {
    MusterTheme {
        Surface {
            MembersTab(
                state = MembersUiState.Success(rows = AdminPreviewRows, canAddMembers = true),
                actions = PreviewActions
            )
        }
    }
}

// 5m
@Preview
@Composable
private fun MembersTabAdminNoTagsPreview() {
    MusterTheme {
        Surface {
            MembersTab(
                state = MembersUiState.Success(rows = AdminPreviewRows, canAddMembers = true, canManageTags = true),
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
    reorder = Reorder(canMoveUp = canMoveUp, canMoveDown = canMoveDown),
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

private val PreviewUntaggedAndInvited = listOf(
    MemberRow(id = "5", displayName = "Conor Blake", status = MemberStatus.Member, canMoveToTag = true, canPromote = true, canRemove = true),
    MemberRow(id = "6", displayName = "Priya Nair", status = MemberStatus.Member, canMoveToTag = true, canPromote = true, canRemove = true),
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
private fun MembersTabAdminWithTagsPreview() {
    MusterTheme {
        Surface {
            MembersTab(
                state = MembersUiState.Success(
                    rows = PreviewUntaggedAndInvited,
                    tags = PreviewTags,
                    canAddMembers = true,
                    canManageTags = true
                ),
                actions = PreviewActions
            )
        }
    }
}

@Preview
@Composable
private fun MembersTabMemberPreview() {
    MusterTheme {
        Surface {
            MembersTab(
                state = MembersUiState.Success(rows = MemberPreviewRows, canAddMembers = false),
                actions = PreviewActions
            )
        }
    }
}

@Preview
@Composable
private fun MembersTabEmptyPreview() {
    MusterTheme {
        Surface {
            MembersTab(
                state = MembersUiState.Success(
                    rows = listOf(MemberRow(id = "1", displayName = "Alex Doyle", status = MemberStatus.Admin, isSelf = true)),
                    canAddMembers = true
                ),
                actions = PreviewActions
            )
        }
    }
}

@Preview
@Composable
private fun MembersTabLoadingPreview() {
    MusterTheme {
        Surface {
            MembersTab(
                state = MembersUiState.Loading,
                actions = PreviewActions,
                spinnerDelayMillis = 0
            )
        }
    }
}

@Preview
@Composable
private fun MembersTabFailedPreview() {
    MusterTheme {
        Surface {
            MembersTab(
                state = MembersUiState.Error(DomainError.Network()),
                actions = PreviewActions
            )
        }
    }
}
