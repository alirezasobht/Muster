package app.muster.domain.usecase

import app.muster.domain.repository.EventRepository

class CancelEventUseCase(private val events: EventRepository) {
    suspend operator fun invoke(eventId: String) = events.cancelEvent(eventId)
}
