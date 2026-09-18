package app.muster.data.fake

import app.muster.domain.error.DomainError
import app.muster.domain.model.Event
import app.muster.domain.repository.EventRepository
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class FakeEventRepository(
    var events: List<Event> = emptyList(),
    var listUpcomingEventsError: DomainError? = null,
    private val latency: Long = FAKE_LATENCY_MS
) : EventRepository {

    override suspend fun listUpcomingEvents(groupId: String): List<Event> {
        delay(latency.milliseconds)
        listUpcomingEventsError?.let { throw it }
        return events.filter { it.groupId == groupId }
    }
}
