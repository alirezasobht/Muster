package app.muster.ui.screens.group.members

import app.muster.domain.error.DomainError
import app.muster.ui.common.util.UiState
import app.muster.ui.common.util.initials

enum class MemberStatus { Admin, Member, Pending }

data class MemberRow(
    val id: String,
    val displayName: String,
    val status: MemberStatus,
    val isSelf: Boolean = false,
    val canPromote: Boolean = false,
    val canDemote: Boolean = false,
    val canRemove: Boolean = false,
    val canRevokeInvitation: Boolean = false,
    val canResendInvitation: Boolean = false,
    val initials: String = displayName.initials()
) {
    val hasMenu: Boolean get() = canPromote || canDemote || canRemove || canRevokeInvitation || canResendInvitation
}

sealed interface MembersUiState : UiState<MembersUiState.Success> {

    data object Loading : MembersUiState

    data class Success(
        val rows: List<MemberRow> = emptyList(),
        val isRefreshing: Boolean = false,
        val actionTargetId: String? = null,
        val actionError: DomainError? = null,
        val failedActionId: String? = null,
        val canAddMembers: Boolean = false,
        val showTagsIntro: Boolean = false
    ) : MembersUiState {
        override fun asSuccessOrNull(): Success = this
        val isEmpty: Boolean get() = rows.none { !it.isSelf }
        val memberCount: Int get() = rows.count { it.status != MemberStatus.Pending }

        // Pending rows are only built for admins, so members always see 0.
        val pendingCount: Int get() = rows.count { it.status == MemberStatus.Pending }
    }

    data class Error(val error: DomainError) : MembersUiState
}
