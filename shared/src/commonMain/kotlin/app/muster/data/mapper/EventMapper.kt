package app.muster.data.mapper

import app.muster.data.dto.EventDto
import app.muster.domain.model.Event
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

internal fun String.toRsvpStatus(): RsvpStatus? = when (this) {
    "pending" -> RsvpStatus.Pending
    "in" -> RsvpStatus.In
    "out" -> RsvpStatus.Out
    else -> null
}
