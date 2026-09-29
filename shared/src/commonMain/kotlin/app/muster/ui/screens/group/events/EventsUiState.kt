package app.muster.ui.screens.group.events

import app.muster.domain.error.DomainError
import app.muster.ui.common.util.UiState

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

sealed interface EventsUiState : UiState<EventsUiState.Success> {
    data object Loading : EventsUiState
    data class Success(
        val eventRows: List<EventRow> = emptyList(),
        val isRefreshing: Boolean = false,
        val canCreateEvent: Boolean = false
    ) : EventsUiState {
        override fun asSuccessOrNull(): Success = this
        val isEmpty: Boolean get() = eventRows.isEmpty()
    }
    data class Error(val error: DomainError) : EventsUiState
}
