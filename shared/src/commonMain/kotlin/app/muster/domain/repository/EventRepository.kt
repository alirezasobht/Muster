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

    // groupId only for the Events(groupId) notification; the RPC takes the
    // event and player alone.
    suspend fun disinvitePlayer(eventId: String, groupId: String, profileId: String)

    // The whole ordered queue, set_standby_order rewrites positions 1..n
    suspend fun reorderStandby(eventId: String, orderedProfileIds: List<String>)

    // add_players_to_event invites while slots remain and queues the rest, in
    // profileIds order. groupId only for the Events(groupId) notification —
    // same reasoning as disinvitePlayer.
    suspend fun addPlayers(eventId: String, groupId: String, profileIds: List<String>)
}
