package app.muster.ui.screens.event

import app.muster.domain.error.DomainError
import app.muster.domain.model.RsvpStatus
import app.muster.ui.common.util.UiState
import app.muster.ui.common.util.initials
import app.muster.ui.screens.group.events.EventRow

enum class RosterAction { SetIn, SetOut, CancelInvite, RemoveFromEvent, RemoveFromList, ResendInvite }

// A standby player has no event_invitations row
data class RosterRow(
    val id: String,
    val name: String,
    val status: RsvpStatus,
    val isSelf: Boolean = false,
    val actions: List<RosterAction> = emptyList(),
    val initials: String = name.initials()
)

data class StandbyRow(
    val id: String,
    val name: String,
    val isSelf: Boolean = false
)

data class EventSummary(
    val groupId: String,
    val groupName: String,
    val eventId: String,
    val title: String,
    val date: String,
    val location: String,
    val capacity: Int,
    val inCount: Int,
    val pendingCount: Int
) {
     companion object {
         fun from(groupId: String, groupName: String, eventRow: EventRow): EventSummary {
             return EventSummary(
                 groupId = groupId,
                 groupName = groupName,
                 eventId = eventRow.id,
                 title = eventRow.title,
                 date = eventRow.date,
                 location = eventRow.location,
                 capacity = eventRow.capacity,
                 inCount = eventRow.inCount,
                 pendingCount = eventRow.pendingCount
             )
         }
     }
}

sealed interface EventUiState : UiState<EventUiState.Success> {
    val summary: EventSummary

    data class Loading(override val summary: EventSummary) : EventUiState

    data class Success(
        override val summary: EventSummary,
        val isAdmin: Boolean,
        // Null = not invited
        val myStatus: RsvpStatus?,
        val roster: List<RosterRow> = emptyList(),
        val standby: List<StandbyRow> = emptyList(),
        val isFrozen: Boolean = false,
        val startTime: String = "",
        val isRefreshing: Boolean = false,
        val rsvpInFlight: Boolean = false,
        val rsvpError: DomainError? = null,
        val rowActionTargetId: String? = null,
        val rowActionError: DomainError? = null,
        val rowActionErrorId: String? = null,
        val standbyReordering: Boolean = false,
        val standbyError: DomainError? = null
    ) : EventUiState {
        override fun asSuccessOrNull(): Success = this
        val isRosterEmpty: Boolean get() = roster.isEmpty()
    }

    data class Error(override val summary: EventSummary, val error: DomainError) : EventUiState
}
