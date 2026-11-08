package app.muster.ui.screens.group.members

import app.muster.domain.error.DomainError
import app.muster.ui.common.util.UiState
import app.muster.ui.common.util.initials
import app.muster.ui.screens.group.members.movetotag.TagOption

enum class MemberStatus { Admin, Member, Pending }

// Move up / Move down are greyed out, not hidden.
data class Reorder(
    val canMoveUp: Boolean,
    val canMoveDown: Boolean
)

data class MemberRow(
    val id: String,
    val displayName: String,
    val status: MemberStatus,
    val isSelf: Boolean = false,
    val tagId: String? = null,
    val canMoveToTag: Boolean = false,
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

    fun inFlight(actionTargetId: String? = null) = id == actionTargetId
}

data class TagSection(
    val id: String,
    val name: String,
    val rows: List<MemberRow>,
    val reorder: Reorder
)

sealed interface MembersTabUiState : UiState<MembersTabUiState.Success> {

    data object Loading : MembersTabUiState

    data class Success(
        // Tagged members, in tag sections. (admin only)
        val tags: List<TagSection> = emptyList(),
        // admin: untagged members
        // member: all members
        val members: List<MemberRow> = emptyList(),
        // Invited, not yet joined. (admin only)
        val pendings: List<MemberRow> = emptyList(),
        val isRefreshing: Boolean = false,
        val actionTargetId: String? = null,
        // The member being dragged to another section; shown there with a spinner.
        val movingProfileId: String? = null,
        val actionError: DomainError? = null,
        val failedActionId: String? = null,
        // A failed Move to tag, shown under the header: the member may sit in a tag card.
        val moveError: DomainError? = null,
        val canAddMembers: Boolean = false,
        val canManageTags: Boolean = false
    ) : MembersTabUiState {
        override fun asSuccessOrNull(): Success = this

        private val allMembers: List<MemberRow> get() = tags.flatMap { it.rows } + members

        val isEmpty: Boolean get() = (allMembers + pendings).none { !it.isSelf }
        val memberCount: Int get() = allMembers.size
        val pendingCount: Int get() = pendings.size

        val showTagsIntro: Boolean get() = canManageTags && tags.isEmpty()
        val showNewTagButton: Boolean get() = canManageTags && tags.isNotEmpty()

        val tagOptions: List<TagOption>
            get() = tags.map {
                TagOption(id = it.id, name = it.name, memberCount = it.rows.size)
            }
    }

    data class Error(val error: DomainError) : MembersTabUiState
}
