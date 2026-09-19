package app.muster.domain.usecase

import app.muster.domain.model.RsvpStatus
import app.muster.domain.repository.EventRepository

class SetRsvpUseCase(private val events: EventRepository) {
    suspend operator fun invoke(eventId: String, profileId: String, status: RsvpStatus) =
        events.setRsvp(eventId, profileId, status)
}
