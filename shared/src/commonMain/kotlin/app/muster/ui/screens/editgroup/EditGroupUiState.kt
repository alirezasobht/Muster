package app.muster.ui.screens.editgroup

import app.muster.domain.error.DomainError
import app.muster.ui.common.isTerminal

data class EditGroupUiState(
    val initialName: String,
    val name: String = initialName,
    val saving: Boolean = false,
    val nameError: DomainError? = null,
    val error: DomainError? = null,
    val saved: Boolean = false
) {
    val isUnchanged: Boolean
        get() = name.trim() == initialName

    val canSave: Boolean
        get() = name.isNotBlank() && !isUnchanged && error?.isTerminal != true
}
