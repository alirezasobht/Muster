package app.muster.ui.screens.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.muster.domain.error.DomainError
import app.muster.domain.event.DataChange
import app.muster.domain.event.DataChanges
import app.muster.domain.model.EventDetail
import app.muster.domain.model.GroupRole
import app.muster.domain.model.RsvpStatus
import app.muster.domain.usecase.GetEventUseCase
import app.muster.domain.usecase.GetMyGroupRoleUseCase
import app.muster.domain.usecase.GetMyProfileUseCase
import app.muster.domain.usecase.SetRsvpUseCase
import app.muster.ui.common.util.toDisplayDate
import app.muster.ui.common.util.toDisplayTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

class EventViewModel(
    initialSummary: EventSummary,
    private val getEvent: GetEventUseCase,
    private val getMyGroupRole: GetMyGroupRoleUseCase,
    private val getMyProfile: GetMyProfileUseCase,
    private val setRsvp: SetRsvpUseCase,
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
        updateSuccess { it.copy(rsvpInFlight = true, rsvpError = null) }
        viewModelScope.launch {
            try {
                setRsvp(eventId, myId, status)
                updateSuccess { it.copy(rsvpInFlight = false) }
            } catch (e: DomainError) {
                updateSuccess { it.copy(rsvpInFlight = false, rsvpError = e) }
            }
        }
    }

    fun onChangeRowStatus(playerId: String, status: RsvpStatus) {
        val current = _state.value as? EventUiState.Success ?: return
        if (current.rowActionTargetId != null) return
        updateSuccess { it.copy(rowActionTargetId = playerId, rowActionError = null) }
        viewModelScope.launch {
            try {
                setRsvp(eventId, playerId, status)
                updateSuccess { it.copy(rowActionTargetId = null) }
            } catch (e: DomainError) {
                updateSuccess { it.copy(rowActionTargetId = null, rowActionError = e) }
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
        if (showIndicator) updateSuccess { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            try {
                val fetched = fetchSuccess(_state.value.summary)
                updateSuccess { fetched.copy(isRefreshing = false) }
            } catch (_: DomainError) {
                updateSuccess { it.copy(isRefreshing = false) }
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
            roster = detail.toRosterRows(myId),
            isFrozen = isFrozen,
            startTime = detail.event.startsAt.toDisplayTime()
        )
    }

    // Reads the latest state at write time. Capturing it before a suspend and
    // copying afterwards loses whatever another in-flight call wrote meanwhile.
    private fun updateSuccess(block: (EventUiState.Success) -> EventUiState.Success) {
        _state.update { current -> if (current is EventUiState.Success) block(current) else current }
    }
}

private val statusOrder = mapOf(RsvpStatus.In to 0, RsvpStatus.Pending to 1, RsvpStatus.Out to 2)

private fun EventDetail.toRosterRows(myProfileId: String): List<RosterRow> = roster
    .map { entry ->
        RosterRow(
            id = entry.profileId,
            name = entry.name,
            status = entry.status,
            isSelf = entry.profileId == myProfileId
        )
    }
    .sortedWith(compareBy({ statusOrder.getValue(it.status) }, { it.name.lowercase() }))
