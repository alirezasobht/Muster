package app.muster.domain.model

import kotlin.time.Instant

// inCount/pendingCount, not a full roster: the Events tab shows counts and
// the viewer's own status, never the other rows. myStatus is null when the
// viewer holds no event_invitations row for this event — not invited, or
// queued on standby.
data class Event(
    val id: String,
    val groupId: String,
    val title: String,
    val startsAt: Instant,
    val location: String?,
    val capacity: Int,
    val inCount: Int,
    val pendingCount: Int,
    val myStatus: RsvpStatus?
)
