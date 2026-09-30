package app.muster.domain.model

import kotlin.time.Instant

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
) {
    companion object
}

fun Event.Companion.emptyEvent(id: String): Event = Event(
    id = id,
    groupId = "",
    title = "",
    startsAt = Instant.DISTANT_PAST,
    location = null,
    capacity = 0,
    inCount = 0,
    pendingCount = 0,
    myStatus = null
)
