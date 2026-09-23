package app.muster.data.mapper

import app.muster.domain.error.DomainError
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.io.IOException

// Every repository call goes through this, so the UI only ever sees DomainError.
// Cancellation must pass through untouched, or cancelled coroutines would
// surface as errors on screen.
internal inline fun <T> mapErrors(block: () -> T): T =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw e.toDomainError()
    }

internal fun Throwable.toDomainError(): DomainError = when (this) {
    is DomainError -> this
    is AuthRestException -> toDomainError()
    is PostgrestRestException -> toDomainError()
    // 429 = Too Many Requests, e.g. from the API gateway rather than Auth.
    is RestException -> if (statusCode == 429) DomainError.RateLimited() else DomainError.Unknown(this)
    // supabase-kt wraps connection failures in HttpRequestException and lets
    // Ktor's timeout through; both are IOException.
    is IOException -> DomainError.Network(this)
    else -> DomainError.Unknown(this)
}

private fun AuthRestException.toDomainError(): DomainError = when (errorCode) {
    AuthErrorCode.OtpExpired -> DomainError.InvalidCode()
    AuthErrorCode.OverEmailSendRateLimit,
    AuthErrorCode.OverRequestRateLimit -> DomainError.RateLimited()
    AuthErrorCode.EmailAddressInvalid,
    AuthErrorCode.ValidationFailed -> DomainError.InvalidEmail()
    AuthErrorCode.SessionNotFound -> DomainError.NotSignedIn()
    // 429 = Too Many Requests: a rate limit whose error code isn't matched above.
    else -> if (statusCode == 429) DomainError.RateLimited() else DomainError.Unknown(this)
}

private fun PostgrestRestException.toDomainError(): DomainError = when (code) {
    // Auto-named from profiles.name's CHECK; renaming the constraint breaks this.
    "23514" -> if ("profiles_name_check" in error) DomainError.InvalidName() else DomainError.Unknown(this)
    // group_invitations_one_pending partial unique index: a second open
    // invitation to the same address in the same group.
    "23505" -> if ("group_invitations_one_pending" in error) DomainError.AlreadyInvited() else DomainError.Unknown(this)
    // plpgsql raise exception with no SQLSTATE: accept/decline_group_invitation,
    // group_keeps_an_admin rejecting a promote/demote/remove/leave,
    // reject_if_already_member rejecting an invite to an existing member,
    // enforce_capacity rejecting an invite/RSVP over capacity, or
    // reject_if_event_started(_self) rejecting a write past starts_at.
    "P0001" -> when {
        "not signed in" in error -> DomainError.NotSignedIn()
        "no pending invitation for you" in error -> DomainError.InvitationNotPending()
        "a group must keep at least one admin" in error -> DomainError.LastAdmin()
        "already a member of this group" in error -> DomainError.AlreadyMember()
        "event is full" in error -> DomainError.EventFull()
        "event has already started" in error -> DomainError.EventFrozen()
        else -> DomainError.Unknown(this)
    }
    // create_group's allowlist guard preserves groups_insert's permission error.
    "42501" -> if ("\"groups\"" in error) DomainError.NotAllowedToCreateGroups() else DomainError.Unknown(this)
    else -> if (statusCode == 401) DomainError.NotSignedIn() else DomainError.Unknown(this)
}
