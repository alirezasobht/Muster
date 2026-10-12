package app.muster.domain.usecase

import app.muster.domain.error.DomainError
import app.muster.domain.model.MusterTimeZone
import app.muster.domain.repository.EventRepository
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toInstant

class UpdateEventUseCase(private val events: EventRepository) {

    // update_event rejects a past start too; checking here gives the field
    // error without a round trip.
    suspend operator fun invoke(
        eventId: String,
        groupId: String,
        title: String,
        date: LocalDate,
        time: LocalTime,
        location: String
    ) {
        require(title.isNotBlank()) { "title must not be blank" }
        val startsAt = LocalDateTime(date, time).toInstant(MusterTimeZone)
        if (startsAt <= Clock.System.now()) throw DomainError.EventStartsInPast()
        events.updateEvent(
            eventId = eventId,
            groupId = groupId,
            title = title.trim(),
            startsAt = startsAt,
            location = location.trim().ifBlank { null }
        )
    }
}
