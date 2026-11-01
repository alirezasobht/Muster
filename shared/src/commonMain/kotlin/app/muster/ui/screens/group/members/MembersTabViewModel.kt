package app.muster.ui.screens.group.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.muster.domain.error.DomainError
import app.muster.domain.event.DataChange
import app.muster.domain.event.DataChanges
import app.muster.domain.model.GroupRole
import app.muster.domain.model.Member
import app.muster.domain.model.MemberListing
import app.muster.domain.model.PendingInvitation
import app.muster.domain.usecase.DeleteTagUseCase
import app.muster.domain.usecase.DemoteMemberUseCase
import app.muster.domain.usecase.GetMyGroupRoleUseCase
import app.muster.domain.usecase.GetMyProfileUseCase
import app.muster.domain.usecase.ListGroupMembersUseCase
import app.muster.domain.usecase.PromoteMemberUseCase
import app.muster.domain.usecase.RemoveMemberUseCase
import app.muster.domain.usecase.ResendInvitationUseCase
import app.muster.domain.usecase.RevokeInvitationUseCase
import app.muster.domain.usecase.SetMemberTagUseCase
import app.muster.ui.common.util.updateSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class MembersTabViewModel(
    private val groupId: String,
    private val listGroupMembers: ListGroupMembersUseCase,
    private val getMyGroupRole: GetMyGroupRoleUseCase,
    private val getMyProfile: GetMyProfileUseCase,
    private val promoteMember: PromoteMemberUseCase,
    private val demoteMember: DemoteMemberUseCase,
    private val removeMember: RemoveMemberUseCase,
    private val revokeInvitation: RevokeInvitationUseCase,
    private val resendInvitation: ResendInvitationUseCase,
    private val deleteTag: DeleteTagUseCase,
    private val setMemberTag: SetMemberTagUseCase,
    dataChanges: DataChanges
) : ViewModel() {

    private val _state = MutableStateFlow<MembersTabUiState>(MembersTabUiState.Loading)
    val state: StateFlow<MembersTabUiState> = _state.asStateFlow()

    // init already loads, so the first resume after launch would load twice.
    private var resumedOnce = false

    init {
        load()
        viewModelScope.launch {
            dataChanges.changes
                .filter { it is DataChange.Members && it.groupId == groupId }
                .collect { refresh(showIndicator = true) }
        }
    }

    fun onRetry() = load()

    fun onResume() {
        if (!resumedOnce) {
            resumedOnce = true
            return
        }
        if (_state.value !is MembersTabUiState.Success) return
        refresh(showIndicator = false)
    }

    fun onRefresh() {
        if (_state.value !is MembersTabUiState.Success) return
        refresh(showIndicator = true)
    }

    fun onPromote(profileId: String) = performAction(profileId) { promoteMember(groupId, profileId) }

    fun onDemote(profileId: String) = performAction(profileId) { demoteMember(groupId, profileId) }

    fun onRemove(profileId: String) = performAction(profileId) { removeMember(groupId, profileId) }

    fun onRevokeInvitation(invitationId: String) = performAction(invitationId) { revokeInvitation(groupId, invitationId) }

    fun onResendInvitation(invitationId: String) = performAction(invitationId) { resendInvitation(groupId, invitationId) }

    fun onDeleteTag(tagId: String) = performAction(tagId) { deleteTag(groupId, tagId) }

    fun onSetMemberTag(
        profileId: String,
        tagId: String?
    ) = performAction(profileId) { setMemberTag(groupId, profileId, tagId) }

    private fun load() {
        _state.value = MembersTabUiState.Loading
        viewModelScope.launch {
            try {
                _state.value = fetchSuccess()
            } catch (e: DomainError) {
                _state.value = MembersTabUiState.Error(e)
            }
        }
    }

    private fun refresh(showIndicator: Boolean) {
        if (showIndicator) _state.updateSuccess { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            try {
                val fetched = fetchSuccess()
                _state.updateSuccess {
                    it.copy(
                        tags = fetched.tags,
                        members = fetched.members,
                        pendings = fetched.pendings,
                        canAddMembers = fetched.canAddMembers,
                        canManageTags = fetched.canManageTags,
                        isRefreshing = false
                    )
                }
            } catch (_: DomainError) {
                _state.updateSuccess { it.copy(isRefreshing = false) }
            }
        }
    }

    // No refetch here: the repository announces the write on DataChanges and the
    // subscription above reloads. Refetching as well would fetch twice.
    private fun performAction(
        targetId: String,
        action: suspend () -> Unit
    ) {
        val current = _state.value as? MembersTabUiState.Success ?: return
        if (current.actionTargetId != null) return
        _state.updateSuccess { it.copy(actionTargetId = targetId, actionError = null, failedActionId = null) }
        viewModelScope.launch {
            try {
                action()
                _state.updateSuccess { it.copy(actionTargetId = null) }
            } catch (e: DomainError) {
                _state.updateSuccess { it.copy(actionTargetId = null, actionError = e, failedActionId = targetId) }
            }
        }
    }

    private suspend fun fetchSuccess(): MembersTabUiState.Success {
        val isAdmin = getMyGroupRole(groupId) == GroupRole.Admin
        val myProfileId = getMyProfile().id
        val listing = listGroupMembers(groupId)
        val members = if (isAdmin) listing.members.filter { it.tagId.isNullOrEmpty() } else listing.members
        // Pending rows are readable by every member (group_invitations_select),
        // but visible to admins only
        return MembersTabUiState.Success(
            tags = if (isAdmin) buildTagSections(listing, myProfileId, isAdmin) else emptyList(),
            members = buildMemberRows(members, myProfileId, isAdmin),
            pendings = if (isAdmin) buildPendingRows(listing.pendingInvitations) else emptyList(),
            canAddMembers = isAdmin,
            canManageTags = isAdmin
        )
    }

    private fun buildTagSections(
        listing: MemberListing,
        myProfileId: String,
        isAdmin: Boolean
    ): List<TagSection> = listing.memberTags.mapIndexed { index, tag ->
        TagSection(
            id = tag.id,
            name = tag.name,
            rows = buildMemberRows(
                members = listing.members.filter { it.tagId == tag.id },
                myProfileId = myProfileId,
                isAdmin = isAdmin
            ),
            reorder = Reorder(
                canMoveUp = index != 0,
                canMoveDown = index != listing.memberTags.size - 1
            )
        )
    }

    private fun buildMemberRows(
        members: List<Member>,
        myProfileId: String,
        isAdmin: Boolean
    ): List<MemberRow> = sortRows(
        members.map { member ->
            val isSelf = member.profileId == myProfileId
            MemberRow(
                id = member.profileId,
                displayName = member.name,
                status = if (member.role == GroupRole.Admin) MemberStatus.Admin else MemberStatus.Member,
                isSelf = isSelf,
                tagId = member.tagId,
                canMoveToTag = isAdmin,
                canPromote = isAdmin && !isSelf && member.role == GroupRole.Member,
                canDemote = isAdmin && !isSelf && member.role == GroupRole.Admin,
                canRemove = isAdmin && !isSelf
            )
        }
    )

    private fun buildPendingRows(pendingInvitations: List<PendingInvitation>): List<MemberRow> = pendingInvitations.map { invitation ->
        MemberRow(
            id = invitation.id,
            displayName = invitation.email,
            status = MemberStatus.Pending,
            tagId = null,
            canRevokeInvitation = true,
            canResendInvitation = true
        )
    }.sortedBy { it.displayName.lowercase() }

    // Self first, then admins A-Z, then members A-Z.
    private fun sortRows(memberRows: List<MemberRow>): List<MemberRow> {
        val (selfRows, otherRows) = memberRows.partition { it.isSelf }
        val admins = otherRows.filter { it.status == MemberStatus.Admin }
            .sortedBy { it.displayName.lowercase() }
        val members = otherRows.filter { it.status == MemberStatus.Member }
            .sortedBy { it.displayName.lowercase() }
        return selfRows + admins + members
    }
}
