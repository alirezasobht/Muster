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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
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
import app.muster.ui.common.components.MusterIcons
import app.muster.ui.common.components.MusterSpinner
import app.muster.ui.common.toMessage
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.content_description_more
import muster.shared.generated.resources.members_action_demote
import muster.shared.generated.resources.members_action_promote
import muster.shared.generated.resources.members_action_remove
import muster.shared.generated.resources.members_action_revoke
import muster.shared.generated.resources.members_add_by_email
import muster.shared.generated.resources.members_empty_body
import muster.shared.generated.resources.members_empty_title
import muster.shared.generated.resources.members_failed_title
import muster.shared.generated.resources.members_remove_body
import muster.shared.generated.resources.members_remove_cancel
import muster.shared.generated.resources.members_remove_confirm
import muster.shared.generated.resources.members_remove_title
import muster.shared.generated.resources.members_status_admin
import muster.shared.generated.resources.members_status_invited
import muster.shared.generated.resources.members_status_member
import muster.shared.generated.resources.members_try_again
import muster.shared.generated.resources.members_you_suffix
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

data class MembersActions(
    val onAddByEmail: () -> Unit,
    val onPromote: (id: String) -> Unit,
    val onDemote: (id: String) -> Unit,
    val onRemove: (id: String) -> Unit,
    val onRevokeInvitation: (id: String) -> Unit,
    val onRetry: () -> Unit,
    val onRefresh: () -> Unit = {}
)

@Composable
fun MembersRoute(
    groupId: String,
    onAddByEmail: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MembersViewModel = koinViewModel { parametersOf(groupId) }
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    val state by viewModel.state.collectAsStateWithLifecycle()
    MembersTab(
        state = state,
        actions = MembersActions(
            onAddByEmail = onAddByEmail,
            onPromote = viewModel::onPromote,
            onDemote = viewModel::onDemote,
            onRemove = viewModel::onRemove,
            onRevokeInvitation = viewModel::onRevokeInvitation,
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

@Composable
private fun MembersContent(
    state: MembersUiState.Success,
    actions: MembersActions,
    actionErrorMessage: String?,
    modifier: Modifier = Modifier
) {
    var pendingRemoval by remember { mutableStateOf<MemberRow?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        if (state.canAddMembers) {
            AddByEmailRow(onClick = actions.onAddByEmail)
            HorizontalDivider(color = MusterColors.Hairline)
        } else {
            Spacer(Modifier.height(8.dp))
        }
        PullToRefreshBox(
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
                    items(state.rows, key = { it.id }) { row ->
                        MemberRowItem(
                            row = row,
                            inFlight = state.actionTargetId == row.id,
                            errorMessage = actionErrorMessage.takeIf { state.failedActionId == row.id },
                            onPromote = { actions.onPromote(row.id) },
                            onDemote = { actions.onDemote(row.id) },
                            onRequestRemove = { pendingRemoval = row },
                            onRevokeInvitation = { actions.onRevokeInvitation(row.id) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }

    pendingRemoval?.let { row ->
        ConfirmDialog(
            title = stringResource(Res.string.members_remove_title, row.displayName),
            body = stringResource(Res.string.members_remove_body),
            confirmText = stringResource(Res.string.members_remove_confirm),
            cancelText = stringResource(Res.string.members_remove_cancel),
            onConfirm = {
                pendingRemoval = null
                actions.onRemove(row.id)
            },
            onDismiss = { pendingRemoval = null }
        )
    }
}

@Composable
private fun AddByEmailRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
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

@Composable
private fun MemberRowItem(
    row: MemberRow,
    inFlight: Boolean,
    errorMessage: String?,
    onPromote: () -> Unit,
    onDemote: () -> Unit,
    onRequestRemove: () -> Unit,
    onRevokeInvitation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPending = row.status == MemberStatus.Pending

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isPending) PendingAvatar() else InitialsAvatar(row.displayName)
            Spacer(Modifier.width(12.dp))
            if (isPending) {
                Text(
                    text = row.displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, color = MusterColors.Secondary),
                    modifier = Modifier.weight(1f)
                )
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = buildAnnotatedString {
                            append(row.displayName)
                            if (row.isSelf) {
                                withStyle(SpanStyle(fontSize = 14.sp, color = MusterColors.Hint, fontWeight = FontWeight.Normal)) {
                                    append(stringResource(Res.string.members_you_suffix))
                                }
                            }
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = stringResource(
                            if (row.status == MemberStatus.Admin) {
                                Res.string.members_status_admin
                            } else {
                                Res.string.members_status_member
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp),
                        color = MusterColors.Muted
                    )
                }
            }
            if (isPending) {
                Spacer(Modifier.width(8.dp))
                InvitedBadge()
            }
            if (row.hasMenu) {
                Spacer(Modifier.width(4.dp))
                RowMenu(
                    row = row,
                    inFlight = inFlight,
                    onPromote = onPromote,
                    onDemote = onDemote,
                    onRemove = onRequestRemove,
                    onRevoke = onRevokeInvitation
                )
            }
        }
        if (errorMessage != null) {
            Spacer(Modifier.height(6.dp))
            Text(text = errorMessage, style = MaterialTheme.typography.bodySmall, color = MusterColors.OutText)
        }
    }
}

// Reads row.canPromote/canDemote/canRemove/canRevokeInvitation as given —
// which of them is true was already decided by the ViewModel.
@Composable
private fun RowMenu(
    row: MemberRow,
    inFlight: Boolean,
    onPromote: () -> Unit,
    onDemote: () -> Unit,
    onRemove: () -> Unit,
    onRevoke: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        if (inFlight) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp).padding(8.dp),
                color = MusterColors.Muted,
                strokeWidth = 2.dp
            )
        } else {
            IconButton(onClick = { expanded = true }, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = MusterIcons.MoreVert,
                    contentDescription = stringResource(Res.string.content_description_more),
                    tint = MusterColors.Muted,
                    modifier = Modifier.size(20.dp)
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (row.canPromote) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.members_action_promote)) },
                        onClick = { expanded = false; onPromote() }
                    )
                }
                if (row.canDemote) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.members_action_demote)) },
                        onClick = { expanded = false; onDemote() }
                    )
                }
                if (row.canRemove) {
                    DropdownMenuItem(
                        text = {
                            Text(text = stringResource(Res.string.members_action_remove), color = MusterColors.OutText)
                        },
                        onClick = { expanded = false; onRemove() }
                    )
                }
                if (row.canRevokeInvitation) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.members_action_revoke)) },
                        onClick = { expanded = false; onRevoke() }
                    )
                }
            }
        }
    }
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

// Compose has no built-in dashed border, so it's drawn by hand — same
// technique as MessageState's DashedIconPlaceholder, circular and smaller.
@Composable
private fun PendingAvatar(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val stroke = Stroke(
        width = with(density) { 1.5.dp.toPx() },
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f))
    )
    Box(
        modifier = modifier.size(40.dp).drawBehind {
            drawOval(color = MusterColors.DashedOutline, style = stroke, size = Size(size.width, size.height))
        },
        contentAlignment = Alignment.Center
    ) {
        Text(text = "@", fontSize = 16.sp, color = MusterColors.Hint)
    }
}

@Composable
private fun InvitedBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        border = BorderStroke(1.dp, MusterColors.Outline),
        color = MusterColors.White
    ) {
        Text(
            text = stringResource(Res.string.members_status_invited),
            style = MaterialTheme.typography.labelSmall,
            color = MusterColors.Muted,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

// Cosmetic only (avatar initials) — not a permission, safe to compute here.
private fun initials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> ""
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}

private val PreviewActions = MembersActions(
    onAddByEmail = {},
    onPromote = {},
    onDemote = {},
    onRemove = {},
    onRevokeInvitation = {},
    onRetry = {}
)

private val AdminPreviewRows = listOf(
    MemberRow(id = "1", displayName = "Alex Doyle", status = MemberStatus.Admin, isSelf = true),
    MemberRow(id = "2", displayName = "Dan Whelan", status = MemberStatus.Admin, canDemote = true, canRemove = true),
    MemberRow(id = "3", displayName = "Marcus Keane", status = MemberStatus.Member, canPromote = true, canRemove = true),
    MemberRow(id = "4", displayName = "Sam Okafor", status = MemberStatus.Member, canPromote = true, canRemove = true),
    MemberRow(id = "5", displayName = "Tomás Neale", status = MemberStatus.Member, canPromote = true, canRemove = true),
    MemberRow(id = "6", displayName = "j.moriarty@outlook.com", status = MemberStatus.Pending, canRevokeInvitation = true),
    MemberRow(id = "7", displayName = "priya.n@gmail.com", status = MemberStatus.Pending, canRevokeInvitation = true)
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
