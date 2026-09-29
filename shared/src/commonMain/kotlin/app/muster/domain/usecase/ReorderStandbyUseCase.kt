package app.muster.domain.usecase

import app.muster.domain.repository.EventRepository

class ReorderStandbyUseCase(private val events: EventRepository) {
    suspend operator fun invoke(
        eventId: String,
        orderedProfileIds: List<String>
    ) = events.reorderStandby(eventId, orderedProfileIds)
}
