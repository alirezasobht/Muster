package app.muster.domain.usecase

import app.muster.domain.model.EventDetail
import app.muster.domain.repository.EventRepository

class GetEventUseCase(private val events: EventRepository) {
    suspend operator fun invoke(eventId: String): EventDetail = events.getEvent(eventId)
}
