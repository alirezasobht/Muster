package app.muster.data.fake

import app.muster.domain.error.DomainError
import app.muster.domain.model.Event
import app.muster.domain.repository.EventRepository
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class FakeEventRepository(
    var events: List<Event> = emptyList(),
    var listUpcomingEventsError: DomainError? = null,
    var createEventError: DomainError? = null,
    private val latency: Long = FAKE_LATENCY_MS
) : EventRepository {

    var createdEventTitles = listOf<String>()
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
        return event
    }
}
