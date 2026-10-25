package app.muster.ui.screens.home.newgroup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.muster.domain.error.DomainError
import app.muster.domain.model.Group
import app.muster.ui.common.components.GroupForm
import app.muster.ui.theme.MusterTheme
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.action_create_group
import muster.shared.generated.resources.new_group_hint
import muster.shared.generated.resources.new_group_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NewGroupRoute(
    onBack: () -> Unit,
    onGroupCreated: (Group) -> Unit,
    viewModel: NewGroupViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.created) {
        state.created?.let {
            onGroupCreated(it)
            viewModel.onCreatedHandled()
        }
    }
    NewGroupScreen(
        name = state.name,
        onNameChange = viewModel::onNameChange,
        onCreate = viewModel::onCreate,
        onBack = onBack,
        creating = state.creating,
        nameError = state.nameError,
        error = state.error,
        canCreate = state.canCreate
    )
}

@Composable
fun NewGroupScreen(
    name: String,
    onNameChange: (String) -> Unit,
    onCreate: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    creating: Boolean = false,
    nameError: DomainError? = null,
    error: DomainError? = null,
    canCreate: Boolean = name.isNotBlank()
) {
    GroupForm(
        screenTitle = stringResource(Res.string.new_group_title),
        actionText = stringResource(Res.string.action_create_group),
        name = name,
        onNameChange = onNameChange,
        onSubmit = onCreate,
        onBack = onBack,
        modifier = modifier,
        hint = stringResource(Res.string.new_group_hint),
        submitting = creating,
        nameError = nameError,
        error = error,
        canSubmit = canCreate
    )
}

@Preview
@Composable
private fun NewGroupScreenEmptyPreview() {
    MusterTheme {
        NewGroupScreen(name = "", onNameChange = {}, onCreate = {}, onBack = {})
    }
}

@Preview
@Composable
private fun NewGroupScreenTypedPreview() {
    MusterTheme {
        NewGroupScreen(name = "Westgate Wednesday 7s", onNameChange = {}, onCreate = {}, onBack = {})
    }
}

@Preview
@Composable
private fun NewGroupScreenCreatingPreview() {
    MusterTheme {
        NewGroupScreen(
            name = "Westgate Wednesday 7s",
            onNameChange = {},
            onCreate = {},
            onBack = {},
            creating = true
        )
    }
}

// 2c: retryable form error, button stays live.
@Preview
@Composable
private fun NewGroupScreenFailedPreview() {
    MusterTheme {
        NewGroupScreen(
            name = "Westgate Wednesday 7s",
            onNameChange = {},
            onCreate = {},
            onBack = {},
            error = DomainError.Network()
        )
    }
}

// 2b: terminal form error, button greys.
@Preview
@Composable
private fun NewGroupScreenNotAllowedPreview() {
    MusterTheme {
        NewGroupScreen(
            name = "Westgate Wednesday 7s",
            onNameChange = {},
            onCreate = {},
            onBack = {},
            error = DomainError.NotAllowedToCreateGroups(),
            canCreate = false
        )
    }
}

@Preview
@Composable
private fun NewGroupScreenNameErrorPreview() {
    MusterTheme {
        NewGroupScreen(
            name = "W",
            onNameChange = {},
            onCreate = {},
            onBack = {},
            nameError = DomainError.InvalidName()
        )
    }
}

// 2d: a server-rejected field alongside a failed request, both shown at once.
@Preview
@Composable
private fun NewGroupScreenNameErrorAndFailedPreview() {
    MusterTheme {
        NewGroupScreen(
            name = "Westgate Wednesday 7s and the Thursday lot",
            onNameChange = {},
            onCreate = {},
            onBack = {},
            nameError = DomainError.InvalidName(),
            error = DomainError.Network()
        )
    }
}
