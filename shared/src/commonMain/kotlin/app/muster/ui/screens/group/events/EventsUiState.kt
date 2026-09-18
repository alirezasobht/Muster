package app.muster.ui.screens.group.events

import app.muster.domain.error.DomainError

enum class MemberEventStatus { In, Out, Pending }

data class EventRow(
    val id: String,
    val title: String,
    val date: String,
    val location: String,
    val capacity: Int,
    val inCount: Int,
    val pendingCount: Int,
    val status: MemberEventStatus?
)

sealed interface EventsUiState {
    data object Loading : EventsUiState
    data class Success(
        val events: List<EventRow> = emptyList(),
        val isRefreshing: Boolean = false,
        val canCreateEvent: Boolean = false
    ) : EventsUiState {
        val isEmpty: Boolean get() = events.isEmpty()
    }
    data class Error(val error: DomainError) : EventsUiState
}