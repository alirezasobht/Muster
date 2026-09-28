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

    // create_group requires profiles.can_create_groups. The entry
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

    // enforce_capacity trigger (P0001, "event is full"): pending + in would
    // exceed capacity. A normal outcome, not an edge case — setting someone
    // from out back to in on a full event always hits this; a slot must
    // free first (SCHEMA.md rule 11, CONTEXT.md Standby).
    class EventFull : DomainError("event is full")

    // reject_if_event_started / _self trigger (P0001, "event has already
    // started"): the RSVP write races the freeze (SCHEMA.md rule 14). The
    // UI hides every control once frozen, so this only fires on that race.
    class EventFrozen : DomainError("event has already started")

    // set_standby_order's own guard (P0001, "queue is out of date, reload"):
    // one of the submitted players was promoted between loading the screen
    // and dropping the drag. The exact race the task calls out — reordering
    // can promote, so the list just sent back can already be stale.
    class StandbyQueueStale : DomainError("standby queue changed, reload")

    // resend_group_invitation's own guard (P0001): last_sent_at falls within
    // today calendar day. Keyed to the invitation row alone, not the
    // caller or the group.
    class InvitationSentTooRecently : DomainError("invitation sent too recently")

    class Unknown(cause: Throwable) : DomainError(cause.message ?: "unknown error", cause)
}
