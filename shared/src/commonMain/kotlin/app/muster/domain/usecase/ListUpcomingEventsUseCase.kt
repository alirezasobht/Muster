package app.muster.domain.usecase

import app.muster.domain.model.Event
import app.muster.domain.repository.EventRepository

class ListUpcomingEventsUseCase(private val events: EventRepository) {
    suspend operator fun invoke(groupId: String): List<Event> = events.listUpcomingEvents(groupId)
}
