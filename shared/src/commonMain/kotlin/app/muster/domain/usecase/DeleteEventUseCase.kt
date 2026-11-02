package app.muster.domain.usecase

import app.muster.domain.repository.EventRepository

class DeleteEventUseCase(private val events: EventRepository) {
    suspend operator fun invoke(
        eventId: String,
        groupId: String
    ) = events.deleteEvent(eventId, groupId)
}
