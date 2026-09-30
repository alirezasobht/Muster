package app.muster.domain.usecase

import app.muster.domain.repository.EventRepository

class AddPlayersUseCase(private val events: EventRepository) {
    suspend operator fun invoke(
        eventId: String,
        groupId: String,
        profileIds: List<String>
    ) = events.addPlayers(eventId, groupId, profileIds)
}
