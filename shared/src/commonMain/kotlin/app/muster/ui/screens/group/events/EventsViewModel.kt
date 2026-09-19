package app.muster.ui.screens.group.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.muster.domain.error.DomainError
import app.muster.domain.event.DataChange
import app.muster.domain.event.DataChanges
import app.muster.domain.model.Event
import app.muster.domain.model.GroupRole
import app.muster.domain.model.RsvpStatus
import app.muster.domain.model.emptyEvent
import app.muster.domain.usecase.GetMyGroupRoleUseCase
import app.muster.domain.usecase.ListUpcomingEventsUseCase
import app.muster.ui.common.util.toDisplayDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EventsViewModel(
    private val groupId: String,
    private val listUpcomingEvents: ListUpcomingEventsUseCase,
    private val getMyGroupRole: GetMyGroupRoleUseCase,
    dataChanges: DataChanges
) : ViewModel() {

    private val _state = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val state: StateFlow<EventsUiState> = _state.asStateFlow()

    // init already loads, so the first resume after launch would load twice.
    private var resumedOnce = false
    private var events: List<Event> = emptyList()

    init {
        load()
        viewModelScope.launch {
            dataChanges.changes
                .filter { it is DataChange.Events && it.groupId == groupId }
                .collect { refresh(showIndicator = true) }
        }
    }

    fun onRetry() = load()

    fun onResume() {
        if (!resumedOnce) {
            resumedOnce = true
            return
        }
        if (_state.value !is EventsUiState.Success) return
        refresh(showIndicator = false)
    }

    fun onRefresh() {
        if (_state.value !is EventsUiState.Success) return
        refresh(showIndicator = true)
    }

    fun getEvent(eventId: String): Event = events.find { it.id == eventId } ?: Event.emptyEvent(id = eventId)

    private fun load() {
        _state.value = EventsUiState.Loading
        viewModelScope.launch {
            try {
                _state.value = fetchSuccess()
            } catch (e: DomainError) {
                _state.value = EventsUiState.Error(e)
            }
        }
    }

    private fun refresh(showIndicator: Boolean) {
        if (showIndicator) updateSuccess { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            try {
                val fetched = fetchSuccess()
                updateSuccess {
                    it.copy(
                        eventRows = fetched.eventRows,
                        canCreateEvent = fetched.canCreateEvent,
                        isRefreshing = false
                    )
                }
            } catch (_: DomainError) {
                updateSuccess { it.copy(isRefreshing = false) }
            }
        }
    }

    private suspend fun fetchSuccess(): EventsUiState.Success {
        val isAdmin = getMyGroupRole(groupId) == GroupRole.Admin
        events = listUpcomingEvents(groupId)
        return EventsUiState.Success(
            eventRows = events.map { it.toRow() },
            canCreateEvent = isAdmin
        )
    }

    // Reads the latest state at write time. Capturing it before a suspend and
    // copying afterwards loses whatever another in-flight call wrote meanwhile.
    private fun updateSuccess(block: (EventsUiState.Success) -> EventsUiState.Success) {
        _state.update { current -> if (current is EventsUiState.Success) block(current) else current }
    }
}

private fun Event.toRow() = EventRow(
    id = id,
    title = title,
    date = startsAt.toDisplayDate(),
    location = location.orEmpty(),
    capacity = capacity,
    inCount = inCount,
    pendingCount = pendingCount,
    status = myStatus?.toMemberEventStatus()
)

private fun RsvpStatus.toMemberEventStatus() = when (this) {
    RsvpStatus.In -> MemberEventStatus.In
    RsvpStatus.Pending -> MemberEventStatus.Pending
    RsvpStatus.Out -> MemberEventStatus.Out
}
