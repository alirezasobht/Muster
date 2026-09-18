package app.muster.domain.repository

import app.muster.domain.model.Event
import kotlin.time.Instant

// Implementations must throw DomainError only; the UI catches nothing else.
interface EventRepository {
    suspend fun listUpcomingEvents(groupId: String): List<Event>

    suspend fun createEvent(
        groupId: String,
        title: String,
        startsAt: Instant,
        location: String?,
        capacity: Int
    ): Event
}
