package app.muster.data.fake

import app.muster.domain.error.DomainError
import app.muster.domain.event.DataChange
import app.muster.domain.event.DataChanges
import app.muster.domain.model.Event
import app.muster.domain.model.EventDetail
import app.muster.domain.model.RsvpStatus
import app.muster.domain.repository.EventRepository
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class FakeEventRepository(
    var events: List<Event> = emptyList(),
    var listUpcomingEventsError: DomainError? = null,
    var createEventError: DomainError? = null,
    var eventDetail: EventDetail = EventDetail(
        event = Event(
            id = "fake-event-1",
            groupId = "fake-group-1",
            title = "Weekly 7-a-side",
            startsAt = Instant.parse("2099-01-01T09:00:00Z"),
            location = "Westgate Pitch 2",
            capacity = 10,
            inCount = 0,
            pendingCount = 0,
            myStatus = null
        ),
        roster = emptyList()
    ),
    var getEventError: DomainError? = null,
    var setRsvpError: DomainError? = null,
    private val dataChanges: DataChanges? = null,
    private val latency: Long = FAKE_LATENCY_MS
) : EventRepository {

    var createdEventTitles = listOf<String>()
        private set

    var rsvpUpdates = listOf<Triple<String, String, RsvpStatus>>()
        private set

    override suspend fun listUpcomingEvents(groupId: String): List<Event> {
        delay(latency.milliseconds)
        listUpcomingEventsError?.let { throw it }
        return events.filter { it.groupId == groupId }
    }

    override suspend fun createEvent(
        groupId: String,
        title: String,
        startsAt: Instant,
        location: String?,
        capacity: Int
    ): Event {
        delay(latency.milliseconds)
        createEventError?.let { throw it }
        val event = Event(
            id = "fake-event-${events.size + 1}",
            groupId = groupId,
            title = title,
            startsAt = startsAt,
            location = location,
            capacity = capacity,
            inCount = 0,
            pendingCount = 0,
            myStatus = null
        )
        events = events + event
        createdEventTitles = createdEventTitles + title
        dataChanges?.notify(DataChange.Events(groupId))
        return event
    }

    override suspend fun getEvent(eventId: String): EventDetail {
        delay(latency.milliseconds)
        getEventError?.let { throw it }
        // Derived fresh from the roster, like the real implementation —
        // never trusted from whatever the fixture set on event, so a
        // setRsvp mutation to the roster can't leave these stale.
        val roster = eventDetail.roster
        val event = eventDetail.event.copy(
            inCount = roster.count { it.status == RsvpStatus.In },
            pendingCount = roster.count { it.status == RsvpStatus.Pending },
            myStatus = roster.firstOrNull { it.profileId == FAKE_USER_ID }?.status
        )
        return eventDetail.copy(event = event)
    }

    override suspend fun setRsvp(eventId: String, profileId: String, status: RsvpStatus) {
        delay(latency.milliseconds)
        setRsvpError?.let { throw it }
        rsvpUpdates = rsvpUpdates + Triple(eventId, profileId, status)
        eventDetail = eventDetail.copy(
            roster = eventDetail.roster.map { entry ->
                if (entry.profileId == profileId) entry.copy(status = status) else entry
            }
        )
        dataChanges?.notify(DataChange.Roster(eventId))
        dataChanges?.notify(DataChange.Events(eventDetail.event.groupId))
    }
}
