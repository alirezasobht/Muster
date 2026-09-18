package app.muster.ui.common.util

import app.muster.domain.model.MusterTimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

// "Wed 17 Sep · 7:00 pm". No stringResource template: weekday/month
// abbreviations and am/pm aren't translated anywhere else in the app yet
// (composeResources has one locale), so this stays a plain formatter rather
// than a half-localized template.
fun Instant.toDisplayDate(): String {
    val dateTime = toLocalDateTime(MusterTimeZone)
    val weekday = dateTime.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    val month = dateTime.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    val hour12 = dateTime.hour.mod(12).let { if (it == 0) 12 else it }
    val amPm = if (dateTime.hour < 12) "am" else "pm"
    val minute = dateTime.minute.toString().padStart(2, '0')
    return "$weekday ${dateTime.day} $month · $hour12:$minute $amPm"
}
