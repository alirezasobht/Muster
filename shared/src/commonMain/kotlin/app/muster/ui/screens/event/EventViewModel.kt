package app.muster.ui.screens.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.muster.domain.error.DomainError
import app.muster.domain.event.DataChange
import app.muster.domain.event.DataChanges
import app.muster.domain.model.EventDetail
import app.muster.domain.model.GroupRole
import app.muster.domain.model.RosterEntry
import app.muster.domain.model.RsvpStatus
import app.muster.domain.usecase.DisinvitePlayerUseCase
import app.muster.domain.usecase.GetEventUseCase
import app.muster.domain.usecase.GetMyGroupRoleUseCase
import app.muster.domain.usecase.GetMyProfileUseCase
import app.muster.domain.usecase.ReorderStandbyUseCase
import app.muster.domain.usecase.ResendEventInvitationUseCase
import app.muster.domain.usecase.SetRsvpUseCase
import app.muster.ui.common.util.toDisplayDate
import app.muster.ui.common.util.toDisplayTime
import app.muster.ui.common.util.updateSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.time.Clock

class EventViewModel(
    initialSummary: EventSummary,
    private val getEvent: GetEventUseCase,
    private val getMyGroupRole: GetMyGroupRoleUseCase,
    private val getMyProfile: GetMyProfileUseCase,
    private val setRsvp: SetRsvpUseCase,
    private val reorderStandby: ReorderStandbyUseCase,
    private val disinvitePlayer: DisinvitePlayerUseCase,
    private val resendEventInvitation: ResendEventInvitationUseCase,
    dataChanges: DataChanges
) : ViewModel() {

    private val eventId = initialSummary.eventId
    private val groupId = initialSummary.groupId

    private val _state = MutableStateFlow<EventUiState>(EventUiState.Loading(initialSummary))
    val state: StateFlow<EventUiState> = _state.asStateFlow()

    // init already loads, so the first resume after launch would load twice.
    private var resumedOnce = false

    // Set once fetchSuccess runs; setRsvp on the own-RSVP block needs it and
    // has no other way to know which roster row is the caller's.
    private var myProfileId: String? = null

    init {
        load()
        viewModelScope.launch {
            dataChanges.changes
                .filter {
                    (it is DataChange.Roster && it.eventId == eventId) ||
                            (it is DataChange.Events && it.groupId == groupId)
                }
                .collect { refresh(showIndicator = true) }
        }
    }

    fun onRetry() = load()

    fun onResume() {
        if (!resumedOnce) {
            resumedOnce = true
            return
        }
        if (_state.value !is EventUiState.Success) return
        refresh(showIndicator = false)
    }

    fun onRefresh() {
        if (_state.value !is EventUiState.Success) return
        refresh(showIndicator = true)
    }

    fun onRsvp(status: RsvpStatus) {
        val current = _state.value as? EventUiState.Success ?: return
        val myId = myProfileId ?: return
        if (current.rsvpInFlight) return
        _state.updateSuccess { it.copy(rsvpInFlight = true, rsvpError = null) }
        viewModelScope.launch {
            try {
                setRsvp(eventId, myId, status)
                _state.updateSuccess { it.copy(rsvpInFlight = false) }
            } catch (e: DomainError) {
                _state.updateSuccess { it.copy(rsvpInFlight = false, rsvpError = e) }
            }
        }
    }

    fun onRosterAction(playerId: String, action: RosterAction) = when (action) {
        RosterAction.SetIn -> onChangeRowStatus(playerId, RsvpStatus.In)
        RosterAction.SetOut -> onChangeRowStatus(playerId, RsvpStatus.Out)
        RosterAction.CancelInvite,
        RosterAction.RemoveFromEvent,
        RosterAction.RemoveFromList -> onDisinvite(playerId)
        RosterAction.ResendInvite -> onResendInvitation(playerId)
    }

    private fun onChangeRowStatus(playerId: String, status: RsvpStatus) {
        val current = _state.value as? EventUiState.Success ?: return
        if (current.rowActionTargetId != null) return
        _state.updateSuccess { it.copy(rowActionTargetId = playerId, rowActionError = null, rowActionErrorId = null) }
        viewModelScope.launch {
            try {
                setRsvp(eventId, playerId, status)
                _state.updateSuccess { it.copy(rowActionTargetId = null) }
            } catch (e: DomainError) {
                _state.updateSuccess { it.copy(rowActionTargetId = null, rowActionError = e, rowActionErrorId = playerId) }
            }
        }
    }

    private fun onDisinvite(playerId: String) {
        val current = _state.value as? EventUiState.Success ?: return
        if (current.rowActionTargetId != null) return
        _state.updateSuccess { it.copy(rowActionTargetId = playerId, rowActionError = null, rowActionErrorId = null) }
        viewModelScope.launch {
            try {
                disinvitePlayer(eventId = eventId, groupId = groupId, profileId = playerId)
                _state.updateSuccess { it.copy(rowActionTargetId = null) }
            } catch (e: DomainError) {
                _state.updateSuccess { it.copy(rowActionTargetId = null, rowActionError = e, rowActionErrorId = playerId) }
            }
        }
    }

    private fun onResendInvitation(playerId: String) {
        val current = _state.value as? EventUiState.Success ?: return
        if (current.rowActionTargetId != null) return
        _state.updateSuccess { it.copy(rowActionTargetId = playerId, rowActionError = null, rowActionErrorId = null) }
        viewModelScope.launch {
            try {
                resendEventInvitation(eventId, playerId)
                _state.updateSuccess { it.copy(rowActionTargetId = null) }
            } catch (e: DomainError) {
                _state.updateSuccess { it.copy(rowActionTargetId = null, rowActionError = e, rowActionErrorId = playerId) }
            }
        }
    }

    fun onReorderStandby(orderedProfileIds: List<String>) {
        val current = _state.value as? EventUiState.Success ?: return
        if (current.standbyReordering) return
        val previousOrder = current.standby
        val optimisticOrder = orderedProfileIds.mapNotNull { id -> previousOrder.find { it.id == id } }
        _state.updateSuccess { it.copy(standby = optimisticOrder, standbyReordering = true, standbyError = null) }
        viewModelScope.launch {
            try {
                reorderStandby(eventId, orderedProfileIds)
                // Already showing the new order; the repository's
                // DataChange.Roster notification and the refresh it triggers
                // only need to confirm it, not apply it.
                _state.updateSuccess { it.copy(standbyReordering = false) }
            } catch (e: DomainError) {
                // The write was rejected — back it out
                _state.updateSuccess { it.copy(standby = previousOrder, standbyReordering = false, standbyError = e) }
            }
        }
    }

    private fun load() {
        val summary = _state.value.summary
        _state.value = EventUiState.Loading(summary)
        viewModelScope.launch {
            try {
                _state.value = fetchSuccess(summary)
            } catch (e: DomainError) {
                _state.value = EventUiState.Error(summary, e)
            }
        }
    }

    private fun refresh(showIndicator: Boolean) {
        if (showIndicator) _state.updateSuccess { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            try {
                val fetched = fetchSuccess(_state.value.summary)
                _state.updateSuccess { fetched.copy(isRefreshing = false) }
            } catch (_: DomainError) {
                _state.updateSuccess { it.copy(isRefreshing = false) }
            }
        }
    }

    private suspend fun fetchSuccess(summary: EventSummary): EventUiState.Success {
        val isAdmin = getMyGroupRole(groupId) == GroupRole.Admin
        val myId = getMyProfile().id
        myProfileId = myId
        val detail = getEvent(eventId)
        val isFrozen = detail.event.startsAt <= Clock.System.now()
        return EventUiState.Success(
            summary = summary.copy(
                title = detail.event.title,
                date = detail.event.startsAt.toDisplayDate(),
                location = detail.event.location.orEmpty(),
                capacity = detail.event.capacity,
                inCount = detail.event.inCount,
                pendingCount = detail.event.pendingCount
            ),
            isAdmin = isAdmin,
            myStatus = detail.event.myStatus,
            roster = detail.toRosterRows(myId, isAdmin),
            standby = detail.toStandbyRows(myId),
            isFrozen = isFrozen,
            startTime = detail.event.startsAt.toDisplayTime()
        )
    }
}

private val statusOrder = mapOf(RsvpStatus.In to 0, RsvpStatus.Pending to 1, RsvpStatus.Out to 2)

private fun EventDetail.toRosterRows(myProfileId: String, isAdmin: Boolean): List<RosterRow> {
    val hasFreeSlot = event.capacity > event.inCount + event.pendingCount
    return roster
        .map { entry ->
            RosterRow(
                id = entry.profileId,
                name = entry.name,
                status = entry.status,
                isSelf = entry.profileId == myProfileId,
                actions = availableActions(entry, myProfileId, isAdmin, hasFreeSlot)
            )
        }
        .sortedWith(compareBy({ statusOrder.getValue(it.status) }, { it.name.lowercase() }))
}

// Queue order, not alphabetical — this list's order is the position, per
// set_standby_order's own contract of rewriting 1..n on every write.
private fun EventDetail.toStandbyRows(myProfileId: String): List<StandbyRow> = standby
    .map { entry ->
        StandbyRow(
            id = entry.profileId,
            name = entry.name,
            isSelf = entry.profileId == myProfileId
        )
    }

private fun availableActions(
    entry: RosterEntry,
    myProfileId: String,
    isAdmin: Boolean,
    hasFreeSlot: Boolean
): List<RosterAction> = buildList {
    if (isAdmin || entry.profileId == myProfileId) {
        when (entry.status) {
            RsvpStatus.Pending -> {
                add(RosterAction.SetIn)
                add(RosterAction.SetOut)
            }
            RsvpStatus.In -> add(RosterAction.SetOut)
            RsvpStatus.Out -> if (hasFreeSlot) add(RosterAction.SetIn)
        }
    }
    if (isAdmin) {
        when (entry.status) {
            RsvpStatus.Pending -> {
                add(RosterAction.ResendInvite)
                add(RosterAction.CancelInvite)
            }
            RsvpStatus.In -> add(RosterAction.RemoveFromEvent)
            RsvpStatus.Out -> add(RosterAction.RemoveFromList)
        }
    }
}


