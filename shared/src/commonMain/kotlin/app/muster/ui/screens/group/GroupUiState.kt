package app.muster.ui.screens.group

import app.muster.domain.error.DomainError
import app.muster.domain.model.GroupRole

enum class GroupTab { Events, Members }

sealed interface GroupUiState {

    val groupName: String

    data class Loading(override val groupName: String) : GroupUiState

    data class Success(
        override val groupName: String,
        val myRole: GroupRole,
        val selectedTab: GroupTab = GroupTab.Events
    ) : GroupUiState {
        val isAdmin: Boolean get() = myRole == GroupRole.Admin
    }

    data class Error(override val groupName: String, val error: DomainError) : GroupUiState
}
