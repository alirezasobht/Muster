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
import muster.shared.generated.resources.members_try_again
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

data class MembersActions(
    val onAddMemberByEmail: () -> Unit,
    val onPromote: (id: String) -> Unit,
    val onDemote: (id: String) -> Unit,
    val onRemove: (id: String) -> Unit,
    val onRevokeInvitation: (id: String) -> Unit,
    val onResendInvitation: (id: String) -> Unit,
    val onRetry: () -> Unit,
    val onRefresh: () -> Unit = {},
    val onNewTag: () -> Unit = {},
    val onMoveToTag: (profileId: String) -> Unit = {},
    val onMoveMemberUp: (profileId: String) -> Unit = {},
    val onMoveMemberDown: (profileId: String) -> Unit = {}
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
            onPromote = viewModel::onPromote,
            onDemote = viewModel::onDemote,
            onRemove = viewModel::onRemove,
            onRevokeInvitation = viewModel::onRevokeInvitation,
            onResendInvitation = viewModel::onResendInvitation,
            onRetry = viewModel::onRetry,
            onRefresh = viewModel::onRefresh
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
                    items(state.rows, key = { it.id }) { row ->
                        MemberRowItem(
                            row = row,
                            actions = MemberRowActions(
                                onMoveToTag = { actions.onMoveToTag(row.id) },
                                onMoveUp = { actions.onMoveMemberUp(row.id) },
                                onMoveDown = { actions.onMoveMemberDown(row.id) },
                                onPromote = {
                                    pendingConfirmation = PendingConfirmation(row, PromoteDialog) { actions.onPromote(row.id) }
                                },
                                onDemote = {
                                    pendingConfirmation = PendingConfirmation(row, DemoteDialog) { actions.onDemote(row.id) }
                                },
                                onRemove = {
                                    pendingConfirmation = PendingConfirmation(row, RemoveDialog) { actions.onRemove(row.id) }
                                },
                                onRevokeInvitation = {
                                    pendingConfirmation = PendingConfirmation(row, RevokeDialog) { actions.onRevokeInvitation(row.id) }
                                },
                                onResendInvitation = {
                                    pendingConfirmation = PendingConfirmation(row, ResendDialog) { actions.onResendInvitation(row.id) }
                                }
                            ),
                            inFlight = state.actionTargetId == row.id,
                            errorMessage = actionErrorMessage.takeIf { state.failedActionId == row.id },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
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
    onPromote = {},
    onDemote = {},
    onRemove = {},
    onRevokeInvitation = {},
    onResendInvitation = {},
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
