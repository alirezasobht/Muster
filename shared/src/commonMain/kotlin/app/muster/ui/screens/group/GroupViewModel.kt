package app.muster.ui.screens.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.muster.domain.error.DomainError
import app.muster.domain.event.DataChange
import app.muster.domain.event.DataChanges
import app.muster.domain.model.GroupRole
import app.muster.domain.usecase.ArchiveGroupUseCase
import app.muster.domain.usecase.GetGroupUseCase
import app.muster.domain.usecase.GetMyGroupRoleUseCase
import app.muster.domain.usecase.LeaveGroupUseCase
import app.muster.ui.common.util.updateSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupViewModel(
    private val groupId: String,
    groupName: String,
    private val getMyGroupRole: GetMyGroupRoleUseCase,
    private val getGroup: GetGroupUseCase,
    private val leaveGroup: LeaveGroupUseCase,
    private val archiveGroup: ArchiveGroupUseCase,
    dataChanges: DataChanges
) : ViewModel() {

    // Starts as the route's name; replaced when the group is edited.
    private var groupName = groupName

    private val _state = MutableStateFlow<GroupUiState>(GroupUiState.Loading(groupName))
    val state: StateFlow<GroupUiState> = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            dataChanges.changes
                .filter { it is DataChange.GroupDetails && it.groupId == groupId }
                .collect { refreshName() }
        }
    }

    fun onRetry() = load()

    fun onTabSelected(tab: GroupTab) {
        _state.updateSuccess { it.copy(selectedTab = tab) }
    }

    // A fresh OverflowDialogState replaces whatever was there, so a stale
    // in-flight flag or error can't outlive its own dialog.
    fun onOverflowActionRequested(action: GroupOverflowAction) {
        _state.updateSuccess { it.copy(overflowDialog = OverflowDialogState(action)) }
    }

    fun onOverflowDialogDismissed() {
        _state.updateSuccess { it.copy(overflowDialog = null) }
    }

    fun onOverflowConfirmed() {
        val current = _state.value as? GroupUiState.Success ?: return
        val dialog = current.overflowDialog ?: return
        if (dialog.inFlight) return
        _state.updateSuccess { it.copy(overflowDialog = dialog.copy(inFlight = true, error = null)) }
        viewModelScope.launch {
            try {
                when (dialog.action) {
                    GroupOverflowAction.Leave -> leaveGroup(groupId)
                    GroupOverflowAction.Archive -> archiveGroup(groupId)
                    // Navigates from the route; never opens a dialog.
                    GroupOverflowAction.Edit -> return@launch
                }
                _state.updateSuccess { it.copy(overflowDialog = null, exitedGroup = true) }
            } catch (e: DomainError) {
                updateOverflowDialog { it.copy(inFlight = false, error = e) }
            }
        }
    }

    fun onExitedHandled() {
        _state.updateSuccess { it.copy(exitedGroup = false) }
    }

    private fun load() {
        _state.value = GroupUiState.Loading(groupName)
        viewModelScope.launch {
            try {
                val role = getMyGroupRole(groupId)
                _state.value = GroupUiState.Success(
                    groupName = groupName,
                    myRole = role,
                    overflowActions = overflowActionsFor(role)
                )
            } catch (e: DomainError) {
                _state.value = GroupUiState.Error(groupName = groupName, error = e)
            }
        }
    }

    private fun refreshName() {
        viewModelScope.launch {
            try {
                val name = getGroup(groupId).name
                groupName = name
                _state.update { it.withGroupName(name) }
            } catch (_: DomainError) {
                // Keeps the old name; the next edit or visit fetches again.
            }
        }
    }

    // Reads the dialog at write time: if it was dismissed while the call was
    // in flight, there is no dialog left to report the failure to.
    private fun updateOverflowDialog(block: (OverflowDialogState) -> OverflowDialogState) {
        _state.updateSuccess { current ->
            val dialog = current.overflowDialog ?: return@updateSuccess current
            current.copy(overflowDialog = block(dialog))
        }
    }
}

private fun overflowActionsFor(role: GroupRole): List<GroupOverflowAction> = buildList {
    if (role == GroupRole.Admin) {
        add(GroupOverflowAction.Edit)
        add(GroupOverflowAction.Archive)
    }
    add(GroupOverflowAction.Leave)
}
