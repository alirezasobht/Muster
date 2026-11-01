package app.muster.ui.screens.group.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.muster.domain.error.DomainError
import app.muster.domain.event.DataChange
import app.muster.domain.event.DataChanges
import app.muster.domain.model.GroupRole
import app.muster.domain.usecase.DeleteTagUseCase
import app.muster.domain.usecase.DemoteMemberUseCase
import app.muster.domain.usecase.GetMyGroupRoleUseCase
import app.muster.domain.usecase.GetMyProfileUseCase
import app.muster.domain.usecase.ListGroupMembersUseCase
import app.muster.domain.usecase.MoveTagDownUseCase
import app.muster.domain.usecase.MoveTagUpUseCase
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
    private val moveTagUp: MoveTagUpUseCase,
    private val moveTagDown: MoveTagDownUseCase,
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

    fun onSetMemberTag(profileId: String, tagId: String?) {
        val current = _state.value as? MembersTabUiState.Success ?: return
        if (current.actionTargetId != null) return
        val row = (current.tags.flatMap { it.rows } + current.members).firstOrNull { it.id == profileId } ?: return
        if (row.tagId.orEmpty() == tagId.orEmpty()) return
        if (!tagId.isNullOrEmpty() && current.tags.none { it.id == tagId }) return
        val previousTags = current.tags
        val previousMembers = current.members
        _state.updateSuccess { it.withMemberMoved(row, tagId) }
        viewModelScope.launch {
            try {
                setMemberTag(groupId, profileId, tagId)
                _state.updateSuccess { it.copy(actionTargetId = null, movingProfileId = null) }
            } catch (e: DomainError) {
                _state.updateSuccess {
                    it.copy(
                        tags = previousTags,
                        members = previousMembers,
                        actionTargetId = null,
                        movingProfileId = null,
                        moveError = e
                    )
                }
            }
        }
    }

    fun onMoveTagUp(tagId: String) = performAction(tagId) { moveTagUp(groupId, currentTagIds(), tagId) }

    fun onMoveTagDown(tagId: String) = performAction(tagId) { moveTagDown(groupId, currentTagIds(), tagId) }

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
        reportsOnRow: Boolean = true,
        action: suspend () -> Unit
    ) {
        val current = _state.value as? MembersTabUiState.Success ?: return
        if (current.actionTargetId != null) return
        _state.updateSuccess { it.copy(actionTargetId = targetId, actionError = null, failedActionId = null, moveError = null) }
        viewModelScope.launch {
            try {
                action()
                _state.updateSuccess { it.copy(actionTargetId = null) }
            } catch (e: DomainError) {
                _state.updateSuccess {
                    if (reportsOnRow) {
                        it.copy(actionTargetId = null, actionError = e, failedActionId = targetId)
                    } else {
                        it.copy(actionTargetId = null, moveError = e)
                    }
                }
            }
        }
    }

    private suspend fun fetchSuccess(): MembersTabUiState.Success {
        val isAdmin = getMyGroupRole(groupId) == GroupRole.Admin
        val myProfileId = getMyProfile().id
        return listGroupMembers(groupId).toSuccessState(myProfileId, isAdmin)
    }

    private fun currentTagIds(): List<String> = (_state.value as? MembersTabUiState.Success)?.tags.orEmpty().map { it.id }
}
