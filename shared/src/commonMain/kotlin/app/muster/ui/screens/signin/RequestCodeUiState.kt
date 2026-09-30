package app.muster.ui.screens.signin

import app.muster.domain.error.DomainError
import app.muster.ui.common.isTerminal

data class RequestCodeUiState(
    val email: String = "",
    val sending: Boolean = false,
    // A wrong address belongs under the field. Everything else — offline,
    // rate limited — is about the request, not the input, and shows above
    // the button.
    val emailError: DomainError? = null,
    val error: DomainError? = null,
    val sentTo: String? = null
) {
    val canSend: Boolean
        get() = email.isNotBlank() && error?.isTerminal != true
}
