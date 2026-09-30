package app.muster.domain.usecase

import app.muster.domain.error.DomainError
import app.muster.domain.model.Event
import app.muster.domain.model.MusterTimeZone
import app.muster.domain.repository.EventRepository
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toInstant

class CreateEventUseCase(private val events: EventRepository) {

    // capacity > 0 is a database CHECK; not duplicated here.
    // The past-start check has no database counterpart to duplicate, so it lives here instead.
    suspend operator fun invoke(
        groupId: String,
        title: String,
        date: LocalDate,
        time: LocalTime,
        location: String,
        capacity: Int
    ): Event {
        // canCreate already blocks a blank title from the UI; reaching here
        // blank means a caller bug, not a user-facing DomainError.
        require(title.isNotBlank()) { "title must not be blank" }
        val startsAt = LocalDateTime(date, time).toInstant(MusterTimeZone)
        if (startsAt <= Clock.System.now()) throw DomainError.EventStartsInPast()
        return events.createEvent(
            groupId = groupId,
            title = title.trim(),
            startsAt = startsAt,
            location = location.trim().ifBlank { null },
            capacity = capacity
        )
    }
}
