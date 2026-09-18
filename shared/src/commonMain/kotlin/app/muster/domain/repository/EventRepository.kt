package app.muster.domain.repository

import app.muster.domain.model.Event

// Implementations must throw DomainError only; the UI catches nothing else.
interface EventRepository {
    suspend fun listUpcomingEvents(groupId: String): List<Event>
}
