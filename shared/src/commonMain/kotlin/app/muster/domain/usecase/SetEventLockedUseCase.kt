package app.muster.domain.usecase

import app.muster.domain.repository.EventRepository

class SetEventLockedUseCase(private val events: EventRepository) {
    suspend operator fun invoke(
        eventId: String,
        locked: Boolean
    ) = events.setEventLocked(eventId, locked)
}
