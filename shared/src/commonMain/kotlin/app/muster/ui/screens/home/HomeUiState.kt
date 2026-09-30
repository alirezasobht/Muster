package app.muster.ui.screens.home

import app.muster.domain.error.DomainError
import app.muster.domain.model.Group
import app.muster.domain.model.GroupInvitation
import app.muster.ui.common.util.UiState

sealed interface HomeUiState : UiState<HomeUiState.Success> {

    data object Loading : HomeUiState

    data class Success(
        val groups: List<Group> = emptyList(),
        val invitations: List<GroupInvitation> = emptyList(),
        val canCreateGroups: Boolean = false,
        val signedInEmail: String = "",
        val isRefreshing: Boolean = false,
        val respondingTo: String? = null,
        val actionError: DomainError? = null,
        val failedInvitationId: String? = null
    ) : HomeUiState {
        override fun asSuccessOrNull(): Success = this
    }

    data class Error(val error: DomainError) : HomeUiState
}
