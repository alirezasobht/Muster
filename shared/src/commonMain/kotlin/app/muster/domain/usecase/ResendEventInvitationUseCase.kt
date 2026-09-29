package app.muster.domain.usecase

import app.muster.domain.repository.EventRepository

class ResendEventInvitationUseCase(private val events: EventRepository) {
    suspend operator fun invoke(
        eventId: String,
        profileId: String
    ) = events.resendInvitation(eventId, profileId)
}
