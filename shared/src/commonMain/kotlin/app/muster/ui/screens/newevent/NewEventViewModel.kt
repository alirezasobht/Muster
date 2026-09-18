package app.muster.ui.screens.newevent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.muster.domain.error.DomainError
import app.muster.domain.usecase.CreateEventUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class NewEventViewModel(
    private val groupId: String,
    private val createEvent: CreateEventUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(NewEventUiState())
    val state: StateFlow<NewEventUiState> = _state.asStateFlow()

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

    fun onCapacityChange(capacity: Int) {
        _state.update { it.copy(capacity = capacity.coerceAtLeast(1), error = null) }
    }

    fun onCreate() {
        val current = _state.value
        val date = current.date
        val time = current.time
        if (!current.canCreate || current.creating || date == null || time == null) return
        // Set before launching, not inside: the guard above must see it on a
        // second tap in the same frame, whatever dispatcher is in play.
        _state.update { it.copy(creating = true, dateTimeError = null, error = null) }
        viewModelScope.launch {
            try {
                val event = createEvent(groupId, current.title, date, time, current.location, current.capacity)
                _state.update { it.copy(creating = false, created = event) }
            } catch (e: DomainError) {
                _state.update {
                    if (e is DomainError.EventStartsInPast) {
                        it.copy(creating = false, dateTimeError = e)
                    } else {
                        it.copy(creating = false, error = e)
                    }
                }
            }
        }
    }

    // Call once routing has acted on `created`, so returning to this screen
    // does not navigate again.
    fun onCreatedHandled() {
        _state.value = NewEventUiState()
    }
}
