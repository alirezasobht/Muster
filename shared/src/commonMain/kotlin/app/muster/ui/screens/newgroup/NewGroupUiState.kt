package app.muster.ui.screens.newgroup

import app.muster.domain.error.DomainError
import app.muster.domain.model.Group
import app.muster.ui.common.isTerminal

data class NewGroupUiState(
    val name: String = "",
    val creating: Boolean = false,
    // A bad name belongs under the field. Everything else — not allowed,
    // offline — is about the request, not the input, and shows above the
    // button.
    val nameError: DomainError? = null,
    val error: DomainError? = null,
    val created: Group? = null
) {
    val canCreate: Boolean
        get() = name.isNotBlank() && error?.isTerminal != true
}
