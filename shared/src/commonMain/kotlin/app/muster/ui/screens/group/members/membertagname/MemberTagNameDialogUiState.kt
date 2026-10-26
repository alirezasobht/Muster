package app.muster.ui.screens.group.members.membertagname

import app.muster.domain.error.DomainError
import app.muster.ui.common.isTerminal

data class MemberTagNameDialogUiState(
    val name: String = "",
    val isRename: Boolean = false,
    // The tag's name when the dialog opened; blank for a new tag.
    val initialName: String = "",
    // Members who keep the tag on rename; 0 for a new tag.
    val memberCount: Int = 0,
    val saving: Boolean = false,
    val nameError: DomainError? = null,
    val error: DomainError? = null
) {
    val isUnchanged: Boolean get() = isRename && name.trim() == initialName

    val canSubmit: Boolean
        get() = name.isNotBlank() && !isUnchanged && error?.isTerminal != true
}
