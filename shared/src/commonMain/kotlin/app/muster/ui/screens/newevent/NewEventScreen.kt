package app.muster.ui.screens.newevent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.muster.domain.error.DomainError
import app.muster.domain.model.Event
import app.muster.domain.model.MusterTimeZone
import app.muster.ui.common.components.FormError
import app.muster.ui.common.components.MusterIcons
import app.muster.ui.common.components.MusterTextField
import app.muster.ui.common.components.PhoneWidth
import app.muster.ui.common.components.PrimaryButton
import app.muster.ui.common.toMessage
import app.muster.ui.theme.MusterColors
import app.muster.ui.theme.MusterTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import muster.shared.generated.resources.Res
import muster.shared.generated.resources.action_create_event
import muster.shared.generated.resources.content_description_back
import muster.shared.generated.resources.field_event_location
import muster.shared.generated.resources.field_event_title
import muster.shared.generated.resources.new_event_capacity_hint
import muster.shared.generated.resources.new_event_capacity_label
import muster.shared.generated.resources.new_event_capacity_value
import muster.shared.generated.resources.new_event_date_label
import muster.shared.generated.resources.new_event_date_placeholder
import muster.shared.generated.resources.new_event_decrease_capacity
import muster.shared.generated.resources.new_event_increase_capacity
import muster.shared.generated.resources.new_event_picker_cancel
import muster.shared.generated.resources.new_event_picker_confirm
import muster.shared.generated.resources.new_event_time_label
import muster.shared.generated.resources.new_event_time_placeholder
import muster.shared.generated.resources.new_event_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Clock
import kotlin.time.Instant

data class NewEventActions(
    val onTitleChange: (String) -> Unit,
    val onDateChange: (LocalDate) -> Unit,
    val onTimeChange: (LocalTime) -> Unit,
    val onLocationChange: (String) -> Unit,
    val onCapacityChange: (Int) -> Unit,
    val onCreate: () -> Unit,
    val onBack: () -> Unit
)

private const val MIN_CAPACITY = 1

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
    Surface(modifier = modifier.fillMaxSize()) {
        PhoneWidth {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = actions.onBack) {
                        Icon(
                            imageVector = MusterIcons.ArrowBack,
                            contentDescription = stringResource(Res.string.content_description_back),
                            tint = MusterColors.Ink,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = stringResource(Res.string.new_event_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(Modifier.height(24.dp))
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    MusterTextField(
                        value = state.title,
                        onValueChange = actions.onTitleChange,
                        label = stringResource(Res.string.field_event_title),
                        enabled = !state.creating,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions()
                    )
                    Spacer(Modifier.height(16.dp))
                    DateTimeRow(state = state, actions = actions)
                    Spacer(Modifier.height(16.dp))
                    MusterTextField(
                        value = state.location,
                        onValueChange = actions.onLocationChange,
                        label = stringResource(Res.string.field_event_location),
                        enabled = !state.creating,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        )
                    )
                    Spacer(Modifier.height(16.dp))
                    CapacityStepper(
                        capacity = state.capacity,
                        enabled = !state.creating,
                        onCapacityChange = actions.onCapacityChange
                    )
                    Spacer(Modifier.height(24.dp))
                    if (state.error != null) {
                        FormError(message = state.error.toMessage())
                        Spacer(Modifier.height(12.dp))
                    }
                    PrimaryButton(
                        text = stringResource(Res.string.action_create_event),
                        onClick = actions.onCreate,
                        enabled = state.canCreate,
                        loading = state.creating
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimeRow(
    state: NewEventUiState,
    actions: NewEventActions,
    modifier: Modifier = Modifier
) {
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        val isDateTimeError = state.dateTimeError != null
        Row {
            PickerField(
                label = stringResource(Res.string.new_event_date_label),
                value = state.date?.toFieldLabel(),
                placeholder = stringResource(Res.string.new_event_date_placeholder),
                enabled = !state.creating,
                isError = isDateTimeError,
                onClick = { showDate = true },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            PickerField(
                label = stringResource(Res.string.new_event_time_label),
                value = state.time?.toFieldLabel(),
                placeholder = stringResource(Res.string.new_event_time_placeholder),
                enabled = !state.creating,
                isError = isDateTimeError,
                onClick = { showTime = true },
                modifier = Modifier.weight(1f)
            )
        }
        if (isDateTimeError) {
            Spacer(Modifier.height(7.dp))
            Text(
                text = state.dateTimeError.toMessage(),
                style = MaterialTheme.typography.bodySmall,
                color = MusterColors.OutText
            )
        }
    }

    if (showDate) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    // The picker returns UTC midnight for the day tapped, so
                    // read it back in UTC. Reading it in Sydney shifts it a day.
                    pickerState.selectedDateMillis
                        ?.let { Instant.fromEpochMilliseconds(it) }
                        ?.toLocalDateTime(TimeZone.UTC)
                        ?.date
                        ?.let(actions.onDateChange)
                    showDate = false
                }) { Text(stringResource(Res.string.new_event_picker_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDate = false }) {
                    Text(stringResource(Res.string.new_event_picker_cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showTime) {
        val initial = state.time ?: Clock.System.now().toLocalDateTime(MusterTimeZone).time
        val pickerState = rememberTimePickerState(
            initialHour = initial.hour,
            initialMinute = initial.minute,
            is24Hour = false
        )
        // TimeInput, not the clock face: it behaves the same on touch, mouse
        // and keyboard, and typing a kickoff time is faster than dragging a
        // dial for a fixture that repeats at the same time every week.
        Dialog(onDismissRequest = { showTime = false }) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MusterColors.White) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimeInput(state = pickerState)
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTime = false }) {
                            Text(stringResource(Res.string.new_event_picker_cancel))
                        }
                        TextButton(onClick = {
                            actions.onTimeChange(LocalTime(pickerState.hour, pickerState.minute))
                            showTime = false
                        }) { Text(stringResource(Res.string.new_event_picker_confirm)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerField(
    label: String,
    value: String?,
    placeholder: String,
    enabled: Boolean,
    isError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MusterColors.Muted)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .border(
                    width = 1.dp,
                    color = if (isError) MusterColors.OutText else MusterColors.Outline,
                    shape = MaterialTheme.shapes.medium
                )
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = value ?: placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = if (value != null) MusterColors.Ink else MusterColors.Hint
            )
        }
    }
}

@Composable
private fun CapacityStepper(
    capacity: Int,
    enabled: Boolean,
    onCapacityChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            text = stringResource(Res.string.new_event_capacity_label),
            style = MaterialTheme.typography.labelMedium,
            color = MusterColors.Muted
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .border(1.dp, MusterColors.Outline, MaterialTheme.shapes.medium)
                .padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.new_event_capacity_value, capacity),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MusterColors.Ink,
                modifier = Modifier.weight(1f)
            )
            StepperButton(
                symbol = "–",
                contentDescription = stringResource(Res.string.new_event_decrease_capacity),
                enabled = enabled && capacity > MIN_CAPACITY,
                onClick = { onCapacityChange(capacity - 1) }
            )
            Spacer(Modifier.width(8.dp))
            StepperButton(
                symbol = "+",
                contentDescription = stringResource(Res.string.new_event_increase_capacity),
                enabled = enabled,
                onClick = { onCapacityChange(capacity + 1) }
            )
        }
        Spacer(Modifier.height(1.dp))
        Text(
            text = stringResource(Res.string.new_event_capacity_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MusterColors.Muted
        )
    }
}

@Composable
private fun StepperButton(
    symbol: String,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(if (enabled) MusterColors.QuietSurface else MusterColors.Hairline, MaterialTheme.shapes.small)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) MusterColors.Ink else MusterColors.Hint
        )
    }
}

private fun LocalDate.toFieldLabel(): String {
    val weekday = dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    val month = month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return "$weekday $day $month"
}

private fun LocalTime.toFieldLabel(): String {
    val hour12 = hour.mod(12).let { if (it == 0) 12 else it }
    val amPm = if (hour < 12) "am" else "pm"
    val minuteStr = minute.toString().padStart(2, '0')
    return "$hour12:$minuteStr $amPm"
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
