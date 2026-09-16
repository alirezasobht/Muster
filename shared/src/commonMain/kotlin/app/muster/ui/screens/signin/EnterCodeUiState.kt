package app.muster.ui.screens.signin

import app.muster.domain.error.DomainError
import app.muster.ui.common.isTerminal

// Must match the Email OTP length in Supabase Auth settings.
const val CODE_LENGTH = 6

data class EnterCodeUiState(
    val email: String = "",
    val code: String = "",
    val verifying: Boolean = false,
    val resendInSeconds: Int = 0,
    // A wrong code belongs under the field. Everything else — offline, rate
    // limited — is about the request, not the input, and shows above the
    // button.
    val codeError: DomainError? = null,
    val error: DomainError? = null
) {
    val canVerify: Boolean
        get() = code.length == CODE_LENGTH && error?.isTerminal != true
}
