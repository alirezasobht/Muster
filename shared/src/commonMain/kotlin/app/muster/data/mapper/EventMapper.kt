package app.muster.data.mapper

import app.muster.data.dto.EventDetailDto
import app.muster.data.dto.EventDto
import app.muster.data.dto.UpcomingEventDto
import app.muster.domain.model.Event
import app.muster.domain.model.EventDetail
import app.muster.domain.model.RosterEntry
import app.muster.domain.model.RsvpStatus
import app.muster.domain.model.StandbyEntry
import kotlin.time.Instant

internal fun EventDto.toEvent(
    inCount: Int,
    pendingCount: Int,
    myStatus: RsvpStatus?
) = Event(
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

internal fun UpcomingEventDto.toEvent() = EventDto(
    id = id,
    groupId = groupId,
    title = title,
    startsAt = startsAt,
    location = location,
    capacity = capacity
).toEvent(
    inCount = inCount,
    pendingCount = pendingCount,
    myStatus = myStatus?.toRsvpStatus()
)

internal fun EventDetailDto.toEventDetail(): EventDetail {
    val rosterEntries = roster.map {
        RosterEntry(profileId = it.profileId, name = it.name.orEmpty(), status = it.status.toRsvpStatus()!!)
    }
    return EventDetail(
        event = event.toEvent(
            inCount = rosterEntries.count { it.status == RsvpStatus.In },
            pendingCount = rosterEntries.count { it.status == RsvpStatus.Pending },
            myStatus = myStatus?.toRsvpStatus()
        ),
        roster = rosterEntries,
        standby = standby.map { StandbyEntry(profileId = it.profileId, name = it.name.orEmpty()) }
    )
}

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
