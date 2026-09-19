package app.muster.data.mapper

import app.muster.data.dto.EventDto
import app.muster.data.dto.EventInvitationRowDto
import app.muster.domain.model.Event
import app.muster.domain.model.RosterEntry
import app.muster.domain.model.RsvpStatus
import kotlin.time.Instant

internal fun EventDto.toEvent(inCount: Int, pendingCount: Int, myStatus: RsvpStatus?) = Event(
    id = id,
    groupId = groupId,
    title = title,
    startsAt = Instant.parse(startsAt),
    location = location,
    capacity = capacity,
    inCount = inCount,
    pendingCount = pendingCount,
    myStatus = myStatus
)

// Names come from a separate profiles lookup, not an embed — see
// ProfileNameRowDto. The CHECK on event_invitations.status guarantees one
// of these three — !! trusts that rather than inventing a fallback for a
// value that can't occur.
internal fun EventInvitationRowDto.toRosterEntry(name: String) = RosterEntry(
    profileId = profileId,
    name = name,
    status = status.toRsvpStatus()!!
)

internal fun String.toRsvpStatus(): RsvpStatus? = when (this) {
    "pending" -> RsvpStatus.Pending
    "in" -> RsvpStatus.In
    "out" -> RsvpStatus.Out
    else -> null
}

internal fun RsvpStatus.toDbValue(): String = when (this) {
    RsvpStatus.Pending -> "pending"
    RsvpStatus.In -> "in"
    RsvpStatus.Out -> "out"
}
