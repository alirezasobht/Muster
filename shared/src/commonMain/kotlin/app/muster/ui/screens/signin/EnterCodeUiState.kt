package app.muster.ui.screens.signin

import app.muster.domain.error.DomainError

// Must match the Email OTP length in Supabase Auth settings.
const val CODE_LENGTH = 6

data class EnterCodeUiState(
    val email: String = "",
    val code: String = "",
    val verifying: Boolean = false,
    val resendInSeconds: Int = 0,
    val error: DomainError? = null
)
