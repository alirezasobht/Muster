package app.muster.ui.screens.editgroup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.muster.domain.error.DomainError
import app.muster.ui.common.components.GroupForm
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.action_save
import muster.shared.generated.resources.edit_group_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

data class EditGroupActions(
    val onNameChange: (String) -> Unit,
    val onSave: () -> Unit,
    val onBack: () -> Unit
)

@Composable
fun EditGroupRoute(
    groupId: String,
    groupName: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditGroupViewModel = koinViewModel { parametersOf(groupId, groupName) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) {
        if (state.saved) {
            onSaved()
            viewModel.onSavedHandled()
        }
    }
    EditGroupScreen(
        state = state,
        actions = EditGroupActions(
            onNameChange = viewModel::onNameChange,
            onSave = viewModel::onSave,
            onBack = onBack
        )
    )
}

@Composable
fun EditGroupScreen(
    state: EditGroupUiState,
    actions: EditGroupActions,
    modifier: Modifier = Modifier
) {
    GroupForm(
        screenTitle = stringResource(Res.string.edit_group_title),
        actionText = stringResource(Res.string.action_save),
        name = state.name,
        onNameChange = actions.onNameChange,
        onSubmit = actions.onSave,
        onBack = actions.onBack,
        modifier = modifier,
        submitting = state.saving,
        nameError = state.nameError,
        error = state.error,
        canSubmit = state.canSave
    )
}

private val PreviewActions = EditGroupActions(onNameChange = {}, onSave = {}, onBack = {})

@Preview
@Composable
private fun EditGroupScreenUnchangedPreview() {
    MusterTheme {
        EditGroupScreen(state = EditGroupUiState(initialName = "Westgate Wednesday 7s"), actions = PreviewActions)
    }
}

@Preview
@Composable
private fun EditGroupScreenChangedPreview() {
    MusterTheme {
        EditGroupScreen(
            state = EditGroupUiState(initialName = "Westgate Wednesday 7s", name = "Westgate Thursday 7s"),
            actions = PreviewActions
        )
    }
}

@Preview
@Composable
private fun EditGroupScreenFailedPreview() {
    MusterTheme {
        EditGroupScreen(
            state = EditGroupUiState(
                initialName = "Westgate Wednesday 7s",
                name = "Westgate Thursday 7s",
                error = DomainError.Network()
            ),
            actions = PreviewActions
        )
    }
}
