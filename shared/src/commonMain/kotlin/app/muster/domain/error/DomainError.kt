package app.muster.domain.error

sealed class DomainError(message: String, cause: Throwable? = null) : Exception(message, cause) {

    class Network(cause: Throwable? = null) : DomainError("network unavailable", cause)

    class NotSignedIn : DomainError("not signed in")

    class InvalidEmail : DomainError("invalid email address")

    // Supabase returns the same error for a wrong code and an expired one.
    class InvalidCode : DomainError("code is wrong or expired")

    class RateLimited : DomainError("rate limited")

    class InvalidName : DomainError("name is blank")

    class Unknown(cause: Throwable) : DomainError(cause.message ?: "unknown error", cause)
}
