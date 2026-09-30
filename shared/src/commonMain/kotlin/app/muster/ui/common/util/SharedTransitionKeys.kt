package app.muster.ui.common.util

object SharedTransitionKeys {
    const val MARK = "muster-mark"
    const val WORDMARK = "muster-wordmark"
    fun groupName(groupId: String) = "group-name-$groupId"
    fun eventTitle(eventId: String) = "event-title-$eventId"
    fun eventDate(eventId: String) = "event-date-$eventId"
    fun eventLocation(eventId: String) = "event-location-$eventId"
    fun eventStats(eventId: String) = "event-stats-$eventId"
}
