package app.muster.ui.screens.group.events.newevent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.muster.domain.error.DomainError
import app.muster.domain.model.Event
import app.muster.ui.common.components.EventForm
import app.muster.ui.common.components.EventFormActions
import app.muster.ui.theme.MusterTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.action_create_event
import muster.shared.generated.resources.new_event_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

data class NewEventActions(
    val onTitleChange: (String) -> Unit,
    val onDateChange: (LocalDate) -> Unit,
    val onTimeChange: (LocalTime) -> Unit,
    val onLocationChange: (String) -> Unit,
    val onCapacityChange: (Int) -> Unit,
    val onCreate: () -> Unit,
    val onBack: () -> Unit
)

@Composable
fun NewEventRoute(
    groupId: String,
    onBack: () -> Unit,
    onEventCreated: (Event) -> Unit,
    viewModel: NewEventViewModel = koinViewModel { parametersOf(groupId) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.created) {
        state.created?.let {
            onEventCreated(it)
            viewModel.onCreatedHandled()
        }
    }
    NewEventScreen(
        state = state,
        actions = NewEventActions(
            onTitleChange = viewModel::onTitleChange,
            onDateChange = viewModel::onDateChange,
            onTimeChange = viewModel::onTimeChange,
            onLocationChange = viewModel::onLocationChange,
            onCapacityChange = viewModel::onCapacityChange,
            onCreate = viewModel::onCreate,
            onBack = onBack
        )
    )
}

@Composable
fun NewEventScreen(
    state: NewEventUiState,
    actions: NewEventActions,
    modifier: Modifier = Modifier
) {
    EventForm(
        screenTitle = stringResource(Res.string.new_event_title),
        actionText = stringResource(Res.string.action_create_event),
        title = state.title,
        date = state.date,
        time = state.time,
        location = state.location,
        capacity = state.capacity,
        actions = EventFormActions(
            onTitleChange = actions.onTitleChange,
            onDateChange = actions.onDateChange,
            onTimeChange = actions.onTimeChange,
            onLocationChange = actions.onLocationChange,
            onCapacityChange = actions.onCapacityChange,
            onSubmit = actions.onCreate,
            onBack = actions.onBack
        ),
        modifier = modifier,
        submitting = state.creating,
        dateTimeError = state.dateTimeError,
        error = state.error,
        canSubmit = state.canCreate
    )
}

@Preview
@Composable
private fun NewEventScreenEmptyPreview() {
    MusterTheme {
        NewEventScreen(state = NewEventUiState(), actions = PreviewActions)
    }
}

@Preview
@Composable
private fun NewEventScreenFilledPreview() {
    MusterTheme {
        NewEventScreen(
            state = NewEventUiState(
                title = "Weekly 7-a-side",
                date = LocalDate(2026, 9, 23),
                time = LocalTime(19, 0),
                location = "Westgate Pitch 2",
                capacity = 10
            ),
            actions = PreviewActions
        )
    }
}

@Preview
@Composable
private fun NewEventScreenSavingPreview() {
    MusterTheme {
        NewEventScreen(
            state = NewEventUiState(
                title = "Weekly 7-a-side",
                date = LocalDate(2026, 9, 23),
                time = LocalTime(19, 0),
                location = "Westgate Pitch 2",
                capacity = 10,
                creating = true
            ),
            actions = PreviewActions
        )
    }
}

// Field error: a start time that has already passed.
@Preview
@Composable
private fun NewEventScreenFieldErrorPreview() {
    MusterTheme {
        NewEventScreen(
            state = NewEventUiState(
                title = "Weekly 7-a-side",
                date = LocalDate(2026, 9, 23),
                time = LocalTime(19, 0),
                location = "Westgate Pitch 2",
                dateTimeError = DomainError.EventStartsInPast()
            ),
            actions = PreviewActions
        )
    }
}

// Form error: a failed, retryable request.
@Preview
@Composable
private fun NewEventScreenFormErrorPreview() {
    MusterTheme {
        NewEventScreen(
            state = NewEventUiState(
                title = "Weekly 7-a-side",
                date = LocalDate(2026, 9, 23),
                time = LocalTime(19, 0),
                location = "Westgate Pitch 2",
                error = DomainError.Network()
            ),
            actions = PreviewActions
        )
    }
}

private val PreviewActions = NewEventActions(
    onTitleChange = {},
    onDateChange = {},
    onTimeChange = {},
    onLocationChange = {},
    onCapacityChange = {},
    onCreate = {},
    onBack = {}
)
