package app.muster.ui.screens.addbyemail

import app.muster.domain.error.DomainError
import app.muster.ui.common.isTerminal

data class AddMemberByEmailUiState(
    val email: String = "",
    val sending: Boolean = false,
    // A bad or already-invited address belongs under the field. Everything
    // else — offline — is about the request, not the input, and shows above
    // the button.
    val emailError: DomainError? = null,
    val error: DomainError? = null,
    // Stays true across rotation/backgrounding until Invite more or Close is
    // tapped — it's what the screen is showing, not a one-shot event.
    val invited: Boolean = false
) {
    val canInvite: Boolean
        get() = email.isNotBlank() && error?.isTerminal != true
}
