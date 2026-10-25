package app.muster.ui.screens.event.editevent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.muster.domain.error.DomainError
import app.muster.domain.usecase.UpdateEventUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class EditEventViewModel(
    private val initial: EditEventInitial,
    private val updateEvent: UpdateEventUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(EditEventUiState.from(initial))
    val state: StateFlow<EditEventUiState> = _state.asStateFlow()

    fun onTitleChange(title: String) {
        _state.update { it.copy(title = title, error = null) }
    }

    fun onDateChange(date: LocalDate) {
        _state.update { it.copy(date = date, dateTimeError = null, error = null) }
    }

    fun onTimeChange(time: LocalTime) {
        _state.update { it.copy(time = time, dateTimeError = null, error = null) }
    }

    fun onLocationChange(location: String) {
        _state.update { it.copy(location = location, error = null) }
    }

    fun onSave() {
        val current = _state.value
        if (!current.canSave || current.saving) return
        _state.update { it.copy(saving = true, dateTimeError = null, error = null) }
        viewModelScope.launch {
            try {
                updateEvent(
                    eventId = initial.eventId,
                    groupId = initial.groupId,
                    title = current.title,
                    date = current.date,
                    time = current.time,
                    location = current.location
                )
                _state.update { it.copy(saving = false, saved = true) }
            } catch (e: DomainError) {
                _state.update {
                    if (e is DomainError.EventStartsInPast) {
                        it.copy(saving = false, dateTimeError = e)
                    } else {
                        it.copy(saving = false, error = e)
                    }
                }
            }
        }
    }

    fun onSavedHandled() {
        _state.update { it.copy(saved = false) }
    }
}
