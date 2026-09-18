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

    // groups insert policy requires profiles.can_create_groups. The entry
    // point is gated on it too, but the flag can change between screens.
    class NotAllowedToCreateGroups : DomainError("not allowed to create groups")

    // group_keeps_an_admin trigger: rejects any change that would leave a
    // group with zero admins — last admin leaving, demoting themselves, or
    // demoting the only other admin.
    class LastAdmin : DomainError("a group must keep at least one admin")

    // group_invitations_one_pending partial unique index (23505): an open
    // invitation to this address in this group already exists.
    class AlreadyInvited : DomainError("already invited to this group")

    // group_invitations_not_member trigger, migration 8 (P0001): the address
    // already holds a group_members row in this group.
    class AlreadyMember : DomainError("already a member of this group")

    // Client-side gate, not a database rule: nothing in the schema stops a
    // past start time. Thrown by CreateEventUseCase, checked against MusterTimeZone.
    class EventStartsInPast : DomainError("event start time must be in the future")

    class Unknown(cause: Throwable) : DomainError(cause.message ?: "unknown error", cause)
}
