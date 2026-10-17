package app.muster.ui.screens.group.members

import app.muster.domain.error.DomainError
import app.muster.ui.common.isTerminal

sealed interface TagNameMode {

    data object New : TagNameMode

    data class Rename(
        val tagId: String,
        val memberCount: Int
    ) : TagNameMode
}

sealed interface TagNameUiState {

    data object Hidden : TagNameUiState

    data class Open(
        val mode: TagNameMode,
        val name: String = "",
        val saving: Boolean = false,
        val nameError: DomainError? = null,
        val error: DomainError? = null
    ) : TagNameUiState {
        val canSubmit: Boolean
            get() = name.isNotBlank() && error?.isTerminal != true
    }
}
