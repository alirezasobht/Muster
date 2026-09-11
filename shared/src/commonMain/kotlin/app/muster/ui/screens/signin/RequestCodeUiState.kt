package app.muster.ui.screens.signin

import app.muster.domain.error.DomainError

data class RequestCodeUiState(
    val email: String = "",
    val sending: Boolean = false,
    // Wrong address: belongs under the field. Everything else — offline, rate
    // limited — is about the request, not the input, and shows below the button.
    val emailError: DomainError? = null,
    val error: DomainError? = null,
    val sentTo: String? = null
)
