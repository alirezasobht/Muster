package app.muster.ui.screens.group

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.muster.domain.error.DomainError
import app.muster.domain.model.Event
import app.muster.domain.model.GroupRole
import app.muster.ui.common.components.ConfirmDialog
import app.muster.ui.common.components.MessageState
import app.muster.ui.common.components.MusterIcons
import app.muster.ui.common.components.MusterSpinner
import app.muster.ui.common.components.PhoneWidth
import app.muster.ui.common.toMessage
import app.muster.ui.common.util.SharedTransitionKeys
import app.muster.ui.common.util.sharedBoundsOrNone
import app.muster.ui.screens.group.events.EventsRoute
import app.muster.ui.screens.group.members.MembersRoute
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.content_description_back
import muster.shared.generated.resources.content_description_more
import muster.shared.generated.resources.group_archive_body
import muster.shared.generated.resources.group_archive_cancel
import muster.shared.generated.resources.group_archive_confirm
import muster.shared.generated.resources.group_archive_title
import muster.shared.generated.resources.group_failed_title
import muster.shared.generated.resources.group_failed_try_again
import muster.shared.generated.resources.group_leave_body
import muster.shared.generated.resources.group_leave_cancel
import muster.shared.generated.resources.group_leave_confirm
import muster.shared.generated.resources.group_leave_title
import muster.shared.generated.resources.group_menu_archive
import muster.shared.generated.resources.group_menu_leave
import muster.shared.generated.resources.group_tab_events
import muster.shared.generated.resources.group_tab_members
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

data class GroupActions(
    val onBack: () -> Unit,
    val onTabSelected: (GroupTab) -> Unit,
    val onRetry: () -> Unit,
    val onOverflowActionRequested: (GroupOverflowAction) -> Unit,
    val onOverflowConfirmed: () -> Unit,
    val onOverflowDialogDismissed: () -> Unit
)

@Composable
fun GroupRoute(
    groupId: String,
    groupName: String,
    onBack: () -> Unit,
    onAddMemberByEmail: () -> Unit,
    onSelectEvent: (groupId: String, groupName: String, event: Event) -> Unit,
    onNewEvent: () -> Unit,
    viewModel: GroupViewModel = koinViewModel { parametersOf(groupId, groupName) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val exited = (state as? GroupUiState.Success)?.exitedGroup == true
    LaunchedEffect(exited) {
        if (exited) {
            onBack()
            viewModel.onExitedHandled()
        }
    }
    GroupScreen(
        groupId = groupId,
        state = state,
        actions = GroupActions(
            onBack = onBack,
            onTabSelected = viewModel::onTabSelected,
            onRetry = viewModel::onRetry,
            onOverflowActionRequested = viewModel::onOverflowActionRequested,
            onOverflowConfirmed = viewModel::onOverflowConfirmed,
            onOverflowDialogDismissed = viewModel::onOverflowDialogDismissed
        ),
        membersContent = {
            MembersRoute(
                groupId = groupId,
                onAddMemberByEmail = onAddMemberByEmail
            )
        },
        eventsContent = {
            EventsRoute(
                groupId = groupId,
                onSelectEvent = { event -> onSelectEvent(groupId, groupName, event) },
                onNewEvent = onNewEvent
            )
        }
    )
}

@Composable
fun GroupScreen(
    state: GroupUiState,
    actions: GroupActions,
    modifier: Modifier = Modifier,
    groupId: String = "",
    spinnerDelayMillis: Long = 400,
    membersContent: @Composable () -> Unit = {},
    eventsContent: @Composable () -> Unit = {}
) {
    Surface(modifier = modifier.fillMaxSize()) {
        PhoneWidth {
            Column(modifier = Modifier.fillMaxSize()) {
                GroupAppBar(
                    groupId = groupId,
                    groupName = state.groupName,
                    overflowActions = (state as? GroupUiState.Success)?.overflowActions ?: emptyList(),
                    overflowDialog = (state as? GroupUiState.Success)?.overflowDialog,
                    onBack = actions.onBack,
                    onOverflowActionRequested = actions.onOverflowActionRequested,
                    onOverflowConfirmed = actions.onOverflowConfirmed,
                    onOverflowDialogDismissed = actions.onOverflowDialogDismissed
                )
                when (state) {
                    is GroupUiState.Loading -> {
                        HorizontalDivider(color = MusterColors.Hairline)
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            MusterSpinner(delayMillis = spinnerDelayMillis)
                        }
                    }

                    is GroupUiState.Error -> {
                        HorizontalDivider(color = MusterColors.Hairline)
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            MessageState(
                                title = stringResource(Res.string.group_failed_title),
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
                                        text = stringResource(Res.string.group_failed_try_again),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }

                    is GroupUiState.Success -> {
                        GroupTabRow(selectedTab = state.selectedTab, onTabSelected = actions.onTabSelected)
                        when (state.selectedTab) {
                            GroupTab.Events -> Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                eventsContent()
                            }

                            GroupTab.Members -> Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                membersContent()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupAppBar(
    groupId: String,
    groupName: String,
    overflowActions: List<GroupOverflowAction>,
    overflowDialog: OverflowDialogState?,
    onBack: () -> Unit,
    onOverflowActionRequested: (GroupOverflowAction) -> Unit,
    onOverflowConfirmed: () -> Unit,
    onOverflowDialogDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = MusterIcons.ArrowBack,
                contentDescription = stringResource(Res.string.content_description_back),
                tint = MusterColors.Ink,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = groupName,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .sharedBoundsOrNone(SharedTransitionKeys.groupName(groupId))
        )
        GroupOverflowMenu(
            actions = overflowActions,
            dialog = overflowDialog,
            onActionRequested = onOverflowActionRequested,
            onConfirm = onOverflowConfirmed,
            onDismiss = onOverflowDialogDismissed
        )
    }
}

@Composable
private fun GroupOverflowMenu(
    actions: List<GroupOverflowAction>,
    dialog: OverflowDialogState?,
    onActionRequested: (GroupOverflowAction) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    if (actions.isNotEmpty()) {
        Box(modifier = modifier) {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = MusterIcons.MoreVert,
                    contentDescription = stringResource(Res.string.content_description_more),
                    tint = MusterColors.Ink,
                    modifier = Modifier.size(24.dp)
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                actions.forEach { action ->
                    DropdownMenuItem(
                        text = { Text(stringResource(action.menuLabel())) },
                        onClick = { expanded = false; onActionRequested(action) }
                    )
                }
            }
        }
    }

    // Stays open across the call: it owns the loading state and, on failure,
    // the error — tapping the same button again is the retry.
    when (dialog?.action) {
        GroupOverflowAction.Leave -> ConfirmDialog(
            title = stringResource(Res.string.group_leave_title),
            body = stringResource(Res.string.group_leave_body),
            confirmText = stringResource(Res.string.group_leave_confirm),
            cancelText = stringResource(Res.string.group_leave_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            confirmLoading = dialog.inFlight,
            errorMessage = dialog.error?.toMessage()
        )

        GroupOverflowAction.Archive -> ConfirmDialog(
            title = stringResource(Res.string.group_archive_title),
            body = stringResource(Res.string.group_archive_body),
            confirmText = stringResource(Res.string.group_archive_confirm),
            cancelText = stringResource(Res.string.group_archive_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            destructive = true,
            confirmLoading = dialog.inFlight,
            errorMessage = dialog.error?.toMessage()
        )

        null -> Unit
    }
}

private fun GroupOverflowAction.menuLabel() = when (this) {
    GroupOverflowAction.Archive -> Res.string.group_menu_archive
    GroupOverflowAction.Leave -> Res.string.group_menu_leave
}

@Composable
private fun GroupTabRow(
    selectedTab: GroupTab,
    onTabSelected: (GroupTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            GroupTabItem(
                label = stringResource(Res.string.group_tab_events),
                selected = selectedTab == GroupTab.Events,
                onClick = { onTabSelected(GroupTab.Events) },
                modifier = Modifier.weight(1f)
            )
            GroupTabItem(
                label = stringResource(Res.string.group_tab_members),
                selected = selectedTab == GroupTab.Members,
                onClick = { onTabSelected(GroupTab.Members) },
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider(color = MusterColors.Hairline)
    }
}

@Composable
private fun GroupTabItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(top = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) MusterColors.Ink else MusterColors.Muted
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(3.dp)
                .background(if (selected) MusterColors.Accent else Color.Transparent)
        )
    }
}

private val PreviewActions = GroupActions(
    onBack = {},
    onTabSelected = {},
    onRetry = {},
    onOverflowActionRequested = {},
    onOverflowConfirmed = {},
    onOverflowDialogDismissed = {}
)
private const val PreviewGroupName = "Westgate Wednesday 7s"

@Preview
@Composable
private fun GroupScreenLoadingPreview() {
    MusterTheme {
        GroupScreen(
            state = GroupUiState.Loading(PreviewGroupName),
            actions = PreviewActions,
            spinnerDelayMillis = 0
        )
    }
}

@Preview
@Composable
private fun GroupScreenFailedPreview() {
    MusterTheme {
        GroupScreen(
            state = GroupUiState.Error(PreviewGroupName, DomainError.Network()),
            actions = PreviewActions
        )
    }
}

@Preview
@Composable
private fun GroupScreenEventsAdminPreview() {
    MusterTheme {
        GroupScreen(
            state = GroupUiState.Success(groupName = PreviewGroupName, myRole = GroupRole.Admin),
            actions = PreviewActions
        )
    }
}

@Preview
@Composable
private fun GroupScreenMembersMemberPreview() {
    MusterTheme {
        GroupScreen(
            state = GroupUiState.Success(
                groupName = PreviewGroupName,
                myRole = GroupRole.Member,
                selectedTab = GroupTab.Members
            ),
            actions = PreviewActions
        )
    }
}
