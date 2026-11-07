package app.muster.ui.screens.event.editevent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.muster.domain.error.DomainError
import app.muster.ui.common.components.EventForm
import app.muster.ui.common.components.EventFormActions
import app.muster.ui.theme.MusterTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.action_save
import muster.shared.generated.resources.edit_event_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EditEventRoute(
    initial: EditEventInitial,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditEventViewModel = koinViewModel { parametersOf(initial) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) {
        if (state.saved) {
            onSaved()
            viewModel.onSavedHandled()
        }
    }
    EditEventScreen(
        state = state,
        actions = EventFormActions(
            onTitleChange = viewModel::onTitleChange,
            onDateChange = viewModel::onDateChange,
            onTimeChange = viewModel::onTimeChange,
            onLocationChange = viewModel::onLocationChange,
            onCapacityChange = {},
            onSubmit = viewModel::onSave,
            onBack = onBack
        )
    )
}

@Composable
fun EditEventScreen(
    state: EditEventUiState,
    actions: EventFormActions,
    modifier: Modifier = Modifier
) {
    EventForm(
        screenTitle = stringResource(Res.string.edit_event_title),
        actionText = stringResource(Res.string.action_save),
        title = state.title,
        date = state.date,
        time = state.time,
        location = state.location,
        capacity = null,
        actions = actions,
        modifier = modifier,
        submitting = state.saving,
        dateTimeError = state.dateTimeError,
        error = state.error,
        canSubmit = state.canSave
    )
}

private val PreviewState = EditEventUiState(
    initialTitle = "Weekly 7-a-side",
    initialDate = LocalDate(2026, 9, 23),
    initialTime = LocalTime(19, 0),
    initialLocation = "Westgate Pitch 2"
)

private val PreviewActions = EventFormActions(
    onTitleChange = {},
    onDateChange = {},
    onTimeChange = {},
    onLocationChange = {},
    onCapacityChange = {},
    onSubmit = {},
    onBack = {}
)

@Preview
@Composable
private fun EditEventScreenUnchangedPreview() {
    MusterTheme {
        EditEventScreen(state = PreviewState, actions = PreviewActions)
    }
}

@Preview
@Composable
private fun EditEventScreenChangedPreview() {
    MusterTheme {
        EditEventScreen(state = PreviewState.copy(time = LocalTime(20, 0)), actions = PreviewActions)
    }
}

@Preview
@Composable
private fun EditEventScreenFrozenPreview() {
    MusterTheme {
        EditEventScreen(
            state = PreviewState.copy(time = LocalTime(20, 0), error = DomainError.EventFrozen()),
            actions = PreviewActions
        )
    }
}
