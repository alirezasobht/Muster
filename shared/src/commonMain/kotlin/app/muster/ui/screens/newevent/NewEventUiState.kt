package app.muster.ui.screens.newevent

import app.muster.domain.error.DomainError
import app.muster.domain.model.Event
import app.muster.ui.common.isTerminal
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class NewEventUiState(
    val title: String = "",
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val location: String = "",
    val capacity: Int = 10,
    val creating: Boolean = false,
    // A past start time belongs under its field; a failed request goes above
    // the button.
    val dateTimeError: DomainError? = null,
    val error: DomainError? = null,
    val created: Event? = null
) {
    val canCreate: Boolean
        get() = title.isNotBlank() && date != null && time != null && error?.isTerminal != true
}
