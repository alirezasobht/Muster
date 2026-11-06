package app.muster.ui.screens.event

import app.muster.domain.error.DomainError
import app.muster.domain.model.RsvpStatus
import app.muster.ui.common.util.UiState
import app.muster.ui.common.util.initials
import app.muster.ui.screens.group.events.EventRow
import kotlin.time.Instant

enum class RosterAction { SetIn, SetOut, CancelInvite, RemoveFromEvent, RemoveFromList, ResendInvite }

enum class EventOverflowAction { Edit, Cancel, Delete }

enum class EventDialogKind { Lock, Unlock, Cancel, Delete }

// Display: cancelled > started > locked.
// Cancelled keeps `started` because nothing is allowed after kickoff.
sealed interface EventPhase {
    data object Active : EventPhase

    data class Locked(val byName: String?) : EventPhase

    data class Started(val atDisplay: String) : EventPhase

    data class Cancelled(
        val byName: String?,
        val started: Boolean
    ) : EventPhase
}

data class EventDialogState(
    val kind: EventDialogKind,
    val inFlight: Boolean = false,
    val error: DomainError? = null
)

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
        fun from(
            groupId: String,
            groupName: String,
            eventRow: EventRow
        ): EventSummary = EventSummary(
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
        val phase: EventPhase = EventPhase.Active,
        private val lockFeatureFlag: Boolean = false,
        private val cancelFeatureFlag: Boolean = false,
        val startsAt: Instant = Instant.DISTANT_FUTURE,
        val isRefreshing: Boolean = false,
        val rsvpInFlight: Boolean = false,
        val rsvpError: DomainError? = null,
        val rowActionTargetId: String? = null,
        val rowActionError: DomainError? = null,
        val rowActionErrorId: String? = null,
        val standbyReordering: Boolean = false,
        val standbyError: DomainError? = null,
        val dialog: EventDialogState? = null,
        val deleted: Boolean = false
    ) : EventUiState {
        override fun asSuccessOrNull(): Success = this
        val isRosterEmpty: Boolean get() = roster.isEmpty()

        val isReadOnly: Boolean get() = phase != EventPhase.Active

        val canToggleLock: Boolean
            get() = isAdmin && lockFeatureFlag && (phase == EventPhase.Active || phase is EventPhase.Locked)

        // Nothing is allowed after kickoff, delete included, cancelled or not.
        val overflowActions: List<EventOverflowAction>
            get() {
                if (!isAdmin) return emptyList()
                val cancel = listOfNotNull(EventOverflowAction.Cancel.takeIf { cancelFeatureFlag })
                return when (phase) {
                    EventPhase.Active -> listOf(EventOverflowAction.Edit) + cancel + EventOverflowAction.Delete
                    is EventPhase.Locked -> cancel + EventOverflowAction.Delete
                    is EventPhase.Cancelled -> if (phase.started) emptyList() else listOf(EventOverflowAction.Delete)
                    is EventPhase.Started -> emptyList()
                }
            }
    }

    data class Error(
        override val summary: EventSummary,
        val error: DomainError
    ) : EventUiState
}
