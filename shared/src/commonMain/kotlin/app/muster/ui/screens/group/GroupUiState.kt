package app.muster.ui.screens.group

import app.muster.domain.error.DomainError
import app.muster.domain.model.GroupRole
import app.muster.ui.common.util.UiState

enum class GroupTab { Events, Members }

enum class GroupOverflowAction { Leave, Archive }

data class OverflowDialogState(
    val action: GroupOverflowAction,
    val inFlight: Boolean = false,
    val error: DomainError? = null
)

sealed interface GroupUiState : UiState<GroupUiState.Success> {

    val groupName: String

    data class Loading(override val groupName: String) : GroupUiState

    data class Success(
        override val groupName: String,
        val myRole: GroupRole,
        val selectedTab: GroupTab = GroupTab.Events,
        val overflowDialog: OverflowDialogState? = null,
        val exitedGroup: Boolean = false
    ) : GroupUiState {
        override fun asSuccessOrNull(): Success = this
        val isAdmin: Boolean get() = myRole == GroupRole.Admin

        val overflowActions: List<GroupOverflowAction>
            get() = buildList {
                if (isAdmin) add(GroupOverflowAction.Archive)
                add(GroupOverflowAction.Leave)
            }
    }

    data class Error(
        override val groupName: String,
        val error: DomainError
    ) : GroupUiState
}
