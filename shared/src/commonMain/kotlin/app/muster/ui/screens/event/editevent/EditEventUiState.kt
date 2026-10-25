package app.muster.ui.screens.event.editevent

import app.muster.domain.error.DomainError
import app.muster.domain.model.MusterTimeZone
import app.muster.ui.common.isTerminal
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toLocalDateTime

// The event as the Event screen last loaded it.
data class EditEventInitial(
    val eventId: String,
    val groupId: String,
    val title: String,
    val startsAt: Instant,
    val location: String
)

data class EditEventUiState(
    val initialTitle: String,
    val initialDate: LocalDate,
    val initialTime: LocalTime,
    val initialLocation: String,
    val title: String = initialTitle,
    val date: LocalDate = initialDate,
    val time: LocalTime = initialTime,
    val location: String = initialLocation,
    val saving: Boolean = false,
    val dateTimeError: DomainError? = null,
    val error: DomainError? = null,
    val saved: Boolean = false
) {
    val isUnchanged: Boolean
        get() = title.trim() == initialTitle &&
            date == initialDate &&
            time == initialTime &&
            location.trim() == initialLocation

    val canSave: Boolean
        get() = title.isNotBlank() && !isUnchanged && error?.isTerminal != true

    companion object {
        fun from(initial: EditEventInitial): EditEventUiState {
            val start = initial.startsAt.toLocalDateTime(MusterTimeZone)
            return EditEventUiState(
                initialTitle = initial.title,
                initialDate = start.date,
                initialTime = LocalTime(start.hour, start.minute),
                initialLocation = initial.location
            )
        }
    }
}
