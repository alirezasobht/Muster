package app.muster.ui.screens.group.members

import app.muster.domain.error.DomainError

enum class MemberStatus { Admin, Member, Pending }

data class MemberRow(
    val id: String,
    val displayName: String,
    val status: MemberStatus,
    val isSelf: Boolean = false,
    val canPromote: Boolean = false,
    val canDemote: Boolean = false,
    val canRemove: Boolean = false,
    val canRevokeInvitation: Boolean = false
) {
    val hasMenu: Boolean get() = canPromote || canDemote || canRemove || canRevokeInvitation
}

sealed interface MembersUiState {

    data object Loading : MembersUiState

    data class Success(
        val rows: List<MemberRow> = emptyList(),
        val isRefreshing: Boolean = false,
        val actionTargetId: String? = null,
        val actionError: DomainError? = null,
        val failedActionId: String? = null,
        val canAddMembers: Boolean = false
    ) : MembersUiState {
        val isEmpty: Boolean get() = rows.none { !it.isSelf }
    }

    data class Error(val error: DomainError) : MembersUiState
}
