package app.muster.ui.screens.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.muster.domain.error.DomainError
import app.muster.domain.usecase.GetMyGroupRoleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupViewModel(
    private val groupId: String,
    private val groupName: String,
    private val getMyGroupRole: GetMyGroupRoleUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<GroupUiState>(GroupUiState.Loading(groupName))
    val state: StateFlow<GroupUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun onRetry() = load()

    fun onTabSelected(tab: GroupTab) {
        updateSuccess { it.copy(selectedTab = tab) }
    }

    private fun load() {
        _state.value = GroupUiState.Loading(groupName)
        viewModelScope.launch {
            try {
                val role = getMyGroupRole(groupId)
                _state.value = GroupUiState.Success(groupName = groupName, myRole = role)
            } catch (e: DomainError) {
                _state.value = GroupUiState.Error(groupName = groupName, error = e)
            }
        }
    }

    private fun updateSuccess(block: (GroupUiState.Success) -> GroupUiState.Success) {
        _state.update { current -> if (current is GroupUiState.Success) block(current) else current }
    }
}
