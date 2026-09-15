package app.muster.ui.common

import app.muster.domain.error.DomainError

// Where a DomainError renders and, for a form error, whether it leaves the
// action button live.
sealed interface ErrorPresentation {

    // Bare text under the field it's about. Unchanged treatment.
    data object Field : ErrorPresentation

    enum class Severity {
        // Network, timeout
        Retryable,

        // Permission, rate limit: retrying the same request fails the same way
        Terminal
    }

    // Filled block above the action button.
    data class Form(val severity: Severity) : ErrorPresentation
}

val DomainError.presentation: ErrorPresentation
    get() = when (this) {
        is DomainError.InvalidEmail,
        is DomainError.InvalidCode,
        is DomainError.InvalidName ->
            ErrorPresentation.Field

        is DomainError.Network,
        is DomainError.Unknown ->
            ErrorPresentation.Form(ErrorPresentation.Severity.Retryable)

        is DomainError.RateLimited,
        is DomainError.NotAllowedToCreateGroups,
        is DomainError.InvitationNotPending,
        is DomainError.LastAdmin,
        is DomainError.NotSignedIn ->
            ErrorPresentation.Form(ErrorPresentation.Severity.Terminal)
    }

// Retrying the same request would fail the same way, so the action stops being offered.
// A field error never is: fixing the field is the retry.
val DomainError.isTerminal: Boolean
    get() = (presentation as? ErrorPresentation.Form)?.severity == ErrorPresentation.Severity.Terminal
