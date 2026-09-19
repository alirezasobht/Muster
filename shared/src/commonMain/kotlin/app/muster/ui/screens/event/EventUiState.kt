package app.muster.ui.screens.event

import app.muster.domain.error.DomainError
import app.muster.domain.model.RsvpStatus
import app.muster.ui.navigation.Group
import app.muster.ui.screens.group.events.EventRow
import app.muster.ui.screens.group.events.MemberEventStatus

// A standby player has no event_invitations row
data class RosterRow(
    val id: String,
    val name: String,
    val status: RsvpStatus,
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

sealed interface EventUiState {
    val summary: EventSummary

    data class Loading(override val summary: EventSummary) : EventUiState

    data class Success(
        override val summary: EventSummary,
        val isAdmin: Boolean,
        // Null = not invited
        val myStatus: RsvpStatus?,
        val roster: List<RosterRow> = emptyList(),
        val isFrozen: Boolean = false,
        val startTime: String = "",
        val isRefreshing: Boolean = false,
        val rsvpInFlight: Boolean = false,
        val rsvpError: DomainError? = null,
        val rowActionTargetId: String? = null,
        val rowActionError: DomainError? = null
    ) : EventUiState {
        val isRosterEmpty: Boolean get() = roster.isEmpty()
    }

    data class Error(override val summary: EventSummary, val error: DomainError) : EventUiState
}
