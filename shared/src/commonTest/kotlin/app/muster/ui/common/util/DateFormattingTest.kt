package app.muster.ui.common.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class DateFormattingTest {

    @Test
    fun formatsWeekdayDayMonthAndTwelveHourTime() {
        assertEquals(
            "Thu 17 Sep · 7:00 pm",
            Instant.parse("2026-09-17T09:00:00Z").toDisplayDate()
        )
    }

    // Sydney is UTC+10 here (AEST, before daylight saving starts), so this
    // also crosses a date boundary — 14:00 UTC the 16th is midnight the 17th.
    @Test
    fun midnightFormatsAsTwelveAm() {
        assertEquals(
            "Thu 17 Sep · 12:00 am",
            Instant.parse("2026-09-16T14:00:00Z").toDisplayDate()
        )
    }

    @Test
    fun noonFormatsAsTwelvePm() {
        assertEquals(
            "Wed 16 Sep · 12:00 pm",
            Instant.parse("2026-09-16T02:00:00Z").toDisplayDate()
        )
    }

    @Test
    fun minutesUnderTenArePadded() {
        assertEquals(
            "Wed 16 Sep · 10:05 am",
            Instant.parse("2026-09-16T00:05:00Z").toDisplayDate()
        )
    }
}
