package app.muster.ui.common.util

import app.muster.domain.model.MusterTimeZone
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toLocalDateTime

fun Instant.toDisplayDate(): String {
    val dateTime = toLocalDateTime(MusterTimeZone)
    val weekday = dateTime.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    val month = dateTime.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return "$weekday ${dateTime.day} $month · ${dateTime.toDisplayTime()}"
}

fun Instant.toDisplayTime(): String = toLocalDateTime(MusterTimeZone).toDisplayTime()

private fun LocalDateTime.toDisplayTime(): String {
    val hour12 = hour.mod(12).let { if (it == 0) 12 else it }
    val amPm = if (hour < 12) "am" else "pm"
    val minute = minute.toString().padStart(2, '0')
    return "$hour12:$minute $amPm"
}
