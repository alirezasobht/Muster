package app.muster.ui.screens.group.members

import app.muster.domain.error.DomainError
import app.muster.ui.common.util.UiState
import app.muster.ui.common.util.initials

enum class MemberStatus { Admin, Member, Pending }

// Move up / Move down are greyed out, not hidden, at the ends.
data class Reorder(
    val canMoveUp: Boolean,
    val canMoveDown: Boolean
)

data class MemberRow(
    val id: String,
    val displayName: String,
    val status: MemberStatus,
    val isSelf: Boolean = false,
    val canMoveToTag: Boolean = false,
    // Null for untagged rows: Untagged is A–Z, so it has no Move up / Move down.
    val reorder: Reorder? = null,
    val canPromote: Boolean = false,
    val canDemote: Boolean = false,
    val canRemove: Boolean = false,
    val canRevokeInvitation: Boolean = false,
    val canResendInvitation: Boolean = false,
    val initials: String = displayName.initials()
) {
    val hasRoleActions: Boolean
        get() = canPromote || canDemote || canRemove || canRevokeInvitation || canResendInvitation

    val hasMenu: Boolean get() = canMoveToTag || hasRoleActions
}

data class TagSection(
    val id: String,
    val name: String,
    val rows: List<MemberRow>,
    val reorder: Reorder
)

sealed interface MembersUiState : UiState<MembersUiState.Success> {

    data object Loading : MembersUiState

    data class Success(
        // Untagged members and pending invites; tagged members live in tags.
        val rows: List<MemberRow> = emptyList(),
        val tags: List<TagSection> = emptyList(),
        val isRefreshing: Boolean = false,
        val actionTargetId: String? = null,
        val actionError: DomainError? = null,
        val failedActionId: String? = null,
        val canAddMembers: Boolean = false,
        val canManageTags: Boolean = false
    ) : MembersUiState {
        override fun asSuccessOrNull(): Success = this

        private val allRows: List<MemberRow> get() = tags.flatMap { it.rows } + rows

        val isEmpty: Boolean get() = allRows.none { !it.isSelf }
        val memberCount: Int get() = allRows.count { it.status != MemberStatus.Pending }

        // Pending rows are only built for admins, so members always see 0.
        val pendingCount: Int get() = invitedRows.size

        val untaggedRows: List<MemberRow> get() = rows.filter { it.status != MemberStatus.Pending }
        val invitedRows: List<MemberRow> get() = rows.filter { it.status == MemberStatus.Pending }

        // 5m: until the first tag exists, the intro panel holds the only New tag button.
        val showTagsIntro: Boolean get() = canManageTags && tags.isEmpty()
        val showNewTagButton: Boolean get() = canManageTags && tags.isNotEmpty()
    }

    data class Error(val error: DomainError) : MembersUiState
}
