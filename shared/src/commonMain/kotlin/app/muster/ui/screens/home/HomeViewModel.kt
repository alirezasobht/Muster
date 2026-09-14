package app.muster.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.muster.domain.error.DomainError
import app.muster.domain.usecase.AcceptGroupInvitationUseCase
import app.muster.domain.usecase.DeclineGroupInvitationUseCase
import app.muster.domain.usecase.GetMyProfileUseCase
import app.muster.domain.usecase.ListMyGroupsUseCase
import app.muster.domain.usecase.ListPendingInvitationsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getMyProfile: GetMyProfileUseCase,
    private val listMyGroups: ListMyGroupsUseCase,
    private val listPendingInvitations: ListPendingInvitationsUseCase,
    private val acceptGroupInvitation: AcceptGroupInvitationUseCase,
    private val declineGroupInvitation: DeclineGroupInvitationUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    // init already loads, so the first resume after launch would load twice.
    private var resumedOnce = false

    init {
        load()
    }

    // 1t's Try again. Only reachable from Error, so there is no list to lose.
    fun onRetry() = load()

    fun onResume() {
        if (!resumedOnce) {
            resumedOnce = true
            return
        }
        if (_state.value !is HomeUiState.Success) return
        refresh(showIndicator = false)
    }

    fun onRefresh() {
        if (_state.value !is HomeUiState.Success) return
        refresh(showIndicator = true)
    }

    fun onAccept(invitationId: String) = respond(invitationId, acceptGroupInvitation::invoke)

    fun onDecline(invitationId: String) = respond(invitationId, declineGroupInvitation::invoke)

    private fun load() {
        _state.value = HomeUiState.Loading
        viewModelScope.launch {
            try {
                val profile = getMyProfile()
                val groups = listMyGroups()
                val invitations = listPendingInvitations()
                _state.value = HomeUiState.Success(
                    groups = groups,
                    invitations = invitations,
                    canCreateGroups = profile.canCreateGroups,
                    signedInEmail = profile.email
                )
            } catch (e: DomainError) {
                _state.value = HomeUiState.Error(e)
            }
        }
    }

    private fun refresh(showIndicator: Boolean) {
        if (showIndicator) updateSuccess { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            try {
                val groups = listMyGroups()
                val invitations = listPendingInvitations()
                updateSuccess {
                    it.copy(groups = groups, invitations = invitations, isRefreshing = false)
                }
            } catch (_: DomainError) {
                updateSuccess { it.copy(isRefreshing = false) }
            }
        }
    }

    private fun respond(invitationId: String, action: suspend (String) -> Unit) {
        val current = _state.value as? HomeUiState.Success ?: return
        if (current.respondingTo != null) return
        updateSuccess { it.copy(respondingTo = invitationId, actionError = null, failedInvitationId = null) }
        viewModelScope.launch {
            try {
                action(invitationId)
                val groups = listMyGroups()
                val invitations = listPendingInvitations()
                updateSuccess {
                    it.copy(groups = groups, invitations = invitations, respondingTo = null)
                }
            } catch (e: DomainError) {
                updateSuccess {
                    it.copy(respondingTo = null, actionError = e, failedInvitationId = invitationId)
                }
            }
        }
    }

    // Reads the latest state at write time. Capturing it before a suspend and
    // copying afterwards loses whatever another in-flight call wrote meanwhile.
    private fun updateSuccess(block: (HomeUiState.Success) -> HomeUiState.Success) {
        _state.update { current -> if (current is HomeUiState.Success) block(current) else current }
    }
}
