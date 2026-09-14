package app.muster.domain.error

sealed class DomainError(message: String, cause: Throwable? = null) : Exception(message, cause) {

    class Network(cause: Throwable? = null) : DomainError("network unavailable", cause)

    class NotSignedIn : DomainError("not signed in")

    class InvalidEmail : DomainError("invalid email address")

    // Supabase returns the same error for a wrong code and an expired one.
    class InvalidCode : DomainError("code is wrong or expired")

    class RateLimited : DomainError("rate limited")

    class InvalidName : DomainError("name is blank")

    // The accept/decline RPCs raise this when the invitation is gone, already
    // answered, or not the caller's.
    class InvitationNotPending : DomainError("invitation is no longer available")

    class Unknown(cause: Throwable) : DomainError(cause.message ?: "unknown error", cause)
}
