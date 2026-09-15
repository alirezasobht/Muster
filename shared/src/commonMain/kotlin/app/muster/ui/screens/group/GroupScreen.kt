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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.muster.domain.error.DomainError
import app.muster.domain.model.GroupRole
import app.muster.ui.common.components.MessageState
import app.muster.ui.common.components.MusterIcons
import app.muster.ui.common.components.MusterSpinner
import app.muster.ui.common.components.PhoneWidth
import app.muster.ui.common.toMessage
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.content_description_back
import muster.shared.generated.resources.content_description_more
import muster.shared.generated.resources.group_events_placeholder
import muster.shared.generated.resources.group_failed_title
import muster.shared.generated.resources.group_failed_try_again
import muster.shared.generated.resources.group_members_placeholder
import muster.shared.generated.resources.group_tab_events
import muster.shared.generated.resources.group_tab_members
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

data class GroupActions(
    val onBack: () -> Unit,
    val onTabSelected: (GroupTab) -> Unit,
    val onRetry: () -> Unit
)

@Composable
fun GroupRoute(
    groupId: String,
    groupName: String,
    onBack: () -> Unit,
    viewModel: GroupViewModel = koinViewModel { parametersOf(groupId, groupName) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GroupScreen(
        state = state,
        actions = GroupActions(
            onBack = onBack,
            onTabSelected = viewModel::onTabSelected,
            onRetry = viewModel::onRetry
        )
    )
}

@Composable
fun GroupScreen(
    state: GroupUiState,
    actions: GroupActions,
    modifier: Modifier = Modifier,
    spinnerDelayMillis: Long = 400
) {
    Surface(modifier = modifier.fillMaxSize()) {
        PhoneWidth {
            Column(modifier = Modifier.fillMaxSize()) {
                GroupAppBar(
                    groupName = state.groupName,
                    isAdmin = (state as? GroupUiState.Success)?.isAdmin == true,
                    onBack = actions.onBack
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
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            val placeholder = when (state.selectedTab) {
                                GroupTab.Events -> stringResource(Res.string.group_events_placeholder)
                                GroupTab.Members -> stringResource(Res.string.group_members_placeholder)
                            }
                            Text(text = placeholder, color = MusterColors.Muted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupAppBar(
    groupName: String,
    isAdmin: Boolean,
    onBack: () -> Unit,
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
            modifier = Modifier.weight(1f)
        )
        // Archive and Leave group land here later (1p). Members-only: nothing yet.
        if (isAdmin) {
            IconButton(onClick = {}) {
                Icon(
                    imageVector = MusterIcons.MoreVert,
                    contentDescription = stringResource(Res.string.content_description_more),
                    tint = MusterColors.Ink,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
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
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 14.dp),
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
                .fillMaxWidth(0.6f)
                .height(2.dp)
                .background(if (selected) MusterColors.Accent else Color.Transparent)
        )
    }
}

private val PreviewActions = GroupActions(onBack = {}, onTabSelected = {}, onRetry = {})
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
