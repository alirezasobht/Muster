package app.muster.ui.screens.addplayers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.muster.domain.error.DomainError
import app.muster.ui.common.components.FormError
import app.muster.ui.common.components.MessageState
import app.muster.ui.common.components.MusterIcons
import app.muster.ui.common.components.MusterSpinner
import app.muster.ui.common.components.PhoneWidth
import app.muster.ui.common.components.PrimaryButton
import app.muster.ui.common.toMessage
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.add_players_confirm
import muster.shared.generated.resources.add_players_destination_invited
import muster.shared.generated.resources.add_players_destination_standby
import muster.shared.generated.resources.add_players_empty_body
import muster.shared.generated.resources.add_players_empty_title
import muster.shared.generated.resources.add_players_failed_title
import muster.shared.generated.resources.add_players_stats
import muster.shared.generated.resources.add_players_title
import muster.shared.generated.resources.add_players_try_again
import muster.shared.generated.resources.content_description_back
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

data class AddPlayersActions(
    val onToggle: (id: String) -> Unit,
    val onConfirm: () -> Unit,
    val onBack: () -> Unit,
    val onRetry: () -> Unit
)

@Composable
fun AddPlayersRoute(
    eventId: String,
    groupId: String,
    onBack: () -> Unit,
    viewModel: AddPlayersViewModel = koinViewModel { parametersOf(eventId, groupId) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.exit.collect { onBack() }
    }
    AddPlayersScreen(
        state = state,
        actions = AddPlayersActions(
            onToggle = viewModel::onToggle,
            onConfirm = viewModel::onConfirm,
            onBack = onBack,
            onRetry = viewModel::onRetry
        )
    )
}

@Composable
fun AddPlayersScreen(
    state: AddPlayersUiState,
    actions: AddPlayersActions,
    modifier: Modifier = Modifier,
    spinnerDelayMillis: Long = 400
) {
    Surface(modifier = modifier.fillMaxSize()) {
        PhoneWidth {
            Column(modifier = Modifier.fillMaxSize()) {
                AddPlayersAppBar(onBack = actions.onBack)
                HorizontalDivider(color = MusterColors.Hairline)
                when (state) {
                    is AddPlayersUiState.Loading -> Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        MusterSpinner(delayMillis = spinnerDelayMillis)
                    }

                    is AddPlayersUiState.Error -> Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        MessageState(
                            title = stringResource(Res.string.add_players_failed_title),
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
                                    text = stringResource(Res.string.add_players_try_again),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }

                    is AddPlayersUiState.Success -> AddPlayersContent(
                        state = state,
                        onToggle = actions.onToggle,
                        onConfirm = actions.onConfirm,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddPlayersAppBar(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.padding(horizontal = 16.dp)) {
            Icon(
                imageVector = MusterIcons.ArrowBack,
                contentDescription = stringResource(Res.string.content_description_back),
                tint = MusterColors.Ink,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = stringResource(Res.string.add_players_title),
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun AddPlayersContent(
    state: AddPlayersUiState.Success,
    onToggle: (String) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(
                Res.string.add_players_stats,
                state.capacity,
                state.freeSlots,
                state.inviting,
                state.standbyPicks
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MusterColors.Muted,
            modifier = Modifier.padding(horizontal = 20.dp).padding(vertical = 14.dp)
        )
        if (state.isEmpty) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                MessageState(
                    title = stringResource(Res.string.add_players_empty_title),
                    body = stringResource(Res.string.add_players_empty_body)
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(state.candidates, key = { it.id }) { candidate ->
                    CandidateRowItem(
                        candidate = candidate,
                        selected = candidate.id in state.selectedIds,
                        destination = state.destinationOf(candidate.id),
                        enabled = !state.adding,
                        onToggle = { onToggle(candidate.id) }
                    )
                }
            }
            HorizontalDivider(color = MusterColors.Hairline)
            Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 26.dp)) {
                if (state.error != null) {
                    FormError(message = state.error.toMessage())
                    Spacer(Modifier.height(12.dp))
                }
                PrimaryButton(
                    text = stringResource(Res.string.add_players_confirm, state.selectedIds.size),
                    onClick = onConfirm,
                    enabled = state.canConfirm,
                    loading = state.adding
                )
            }
        }
    }
}

@Composable
private fun CandidateRowItem(
    candidate: CandidateRow,
    selected: Boolean,
    destination: PickDestination?,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SelectionBox(selected = selected)
        Spacer(Modifier.width(14.dp))
        InitialsAvatar(candidate.initials)
        Spacer(Modifier.width(12.dp))
        Text(
            text = candidate.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (destination != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = when (destination) {
                    PickDestination.Invited -> stringResource(Res.string.add_players_destination_invited)
                    is PickDestination.Standby -> stringResource(Res.string.add_players_destination_standby, destination.position)
                },
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.5.sp),
                color = MusterColors.Muted
            )
        }
    }
}

@Composable
private fun SelectionBox(selected: Boolean, modifier: Modifier = Modifier) {
    if (selected) {
        Surface(
            modifier = modifier.size(22.dp),
            shape = RoundedCornerShape(6.dp),
            color = MusterColors.Accent
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "✓",
                    color = MusterColors.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .size(22.dp)
                .border(1.5.dp, MusterColors.Outline, RoundedCornerShape(6.dp))
        )
    }
}

@Composable
private fun InitialsAvatar(initials: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(34.dp),
        shape = RoundedCornerShape(percent = 50),
        color = MusterColors.AvatarFill
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = initials,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                color = MusterColors.Secondary
            )
        }
    }
}

private val PreviewActions = AddPlayersActions(
    onToggle = {},
    onConfirm = {},
    onBack = {},
    onRetry = {}
)

private val PreviewCandidates = listOf(
    CandidateRow(id = "1", name = "Leo Fanning"),
    CandidateRow(id = "2", name = "Hana Berg"),
    CandidateRow(id = "3", name = "Eoin O’Shea"),
    CandidateRow(id = "4", name = "Nadia Rahim"),
    CandidateRow(id = "5", name = "Conor Blake")
)

// 1o: several selectable, none picked yet.
@Preview
@Composable
private fun AddPlayersScreenSelectablePreview() {
    MusterTheme {
        AddPlayersScreen(
            state = AddPlayersUiState.Success(
                candidates = PreviewCandidates,
                capacity = 12,
                freeSlots = 3,
                queueLength = 0
            ),
            actions = PreviewActions
        )
    }
}

// 1o: event full, two picked — matches the design frame exactly
// (Standby #4, Standby #5).
@Preview
@Composable
private fun AddPlayersScreenSomeSelectedPreview() {
    MusterTheme {
        AddPlayersScreen(
            state = AddPlayersUiState.Success(
                candidates = PreviewCandidates,
                selectedIds = listOf("1", "2"),
                capacity = 12,
                freeSlots = 0,
                queueLength = 3
            ),
            actions = PreviewActions
        )
    }
}

// Every group member is already invited or queued.
@Preview
@Composable
private fun AddPlayersScreenEmptyPreview() {
    MusterTheme {
        AddPlayersScreen(
            state = AddPlayersUiState.Success(candidates = emptyList()),
            actions = PreviewActions
        )
    }
}

// Mid-submit: one lands Invited, one Standby — rows disabled, button spinning.
@Preview
@Composable
private fun AddPlayersScreenAddingPreview() {
    MusterTheme {
        AddPlayersScreen(
            state = AddPlayersUiState.Success(
                candidates = PreviewCandidates,
                selectedIds = listOf("1", "2", "3"),
                capacity = 12,
                freeSlots = 2,
                queueLength = 0,
                adding = true
            ),
            actions = PreviewActions
        )
    }
}

@Preview
@Composable
private fun AddPlayersScreenErrorPreview() {
    MusterTheme {
        AddPlayersScreen(
            state = AddPlayersUiState.Success(
                candidates = PreviewCandidates,
                selectedIds = listOf("1"),
                capacity = 12,
                freeSlots = 1,
                error = DomainError.Network()
            ),
            actions = PreviewActions
        )
    }
}
