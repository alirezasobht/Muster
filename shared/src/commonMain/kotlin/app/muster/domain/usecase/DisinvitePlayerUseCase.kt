package app.muster.domain.usecase

import app.muster.domain.repository.EventRepository

class DisinvitePlayerUseCase(private val events: EventRepository) {
    suspend operator fun invoke(eventId: String, groupId: String, profileId: String) =
        events.disinvitePlayer(eventId, groupId, profileId)
}