package app.muster.ui.screens.group

import app.muster.domain.error.DomainError
import app.muster.domain.model.GroupRole
import app.muster.ui.common.util.UiState

enum class GroupTab { Events, Members }

enum class GroupOverflowAction { Edit, Archive, Leave }

data class OverflowDialogState(
    val action: GroupOverflowAction,
    val inFlight: Boolean = false,
    val error: DomainError? = null
)

sealed interface GroupUiState : UiState<GroupUiState.Success> {

    val groupName: String

    fun withGroupName(groupName: String): GroupUiState

    data class Loading(override val groupName: String) : GroupUiState {
        override fun withGroupName(groupName: String) = copy(groupName = groupName)
    }

    data class Success(
        override val groupName: String,
        val myRole: GroupRole,
        // Set by the ViewModel from the viewer's role; the menu only renders it.
        val overflowActions: List<GroupOverflowAction> = emptyList(),
        val selectedTab: GroupTab = GroupTab.Events,
        val overflowDialog: OverflowDialogState? = null,
        val exitedGroup: Boolean = false
    ) : GroupUiState {
        override fun asSuccessOrNull(): Success = this
        override fun withGroupName(groupName: String) = copy(groupName = groupName)
        val isAdmin: Boolean get() = myRole == GroupRole.Admin
    }

    data class Error(
        override val groupName: String,
        val error: DomainError
    ) : GroupUiState {
        override fun withGroupName(groupName: String) = copy(groupName = groupName)
    }
}
