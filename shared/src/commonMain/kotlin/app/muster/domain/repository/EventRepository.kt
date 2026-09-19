package app.muster.domain.repository

import app.muster.domain.model.Event
import app.muster.domain.model.EventDetail
import app.muster.domain.model.RsvpStatus
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

    suspend fun getEvent(eventId: String): EventDetail

    // profileId is the caller for a self-RSVP, or the target player when an
    // admin changes someone else's — RLS is what actually tells them apart.
    suspend fun setRsvp(eventId: String, profileId: String, status: RsvpStatus)
}
