package app.muster.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EventDto(
    val id: String,
    @SerialName("group_id") val groupId: String,
    val title: String,
    @SerialName("starts_at") val startsAt: String,
    val location: String? = null,
    val capacity: Int,
    @SerialName("locked_at") val lockedAt: String? = null,
    // get_event_detail only; the events row returned by the write RPCs has
    // locked_by instead. Same for cancelled_by_name.
    @SerialName("locked_by_name") val lockedByName: String? = null,
    @SerialName("cancelled_at") val cancelledAt: String? = null,
    @SerialName("cancelled_by_name") val cancelledByName: String? = null
)

@Serializable
internal data class ListUpcomingEventsDto(@SerialName("gid") val groupId: String)

@Serializable
internal data class UpcomingEventDto(
    val id: String,
    @SerialName("group_id") val groupId: String,
    val title: String,
    @SerialName("starts_at") val startsAt: String,
    val location: String? = null,
    val capacity: Int,
    @SerialName("locked_at") val lockedAt: String? = null,
    @SerialName("cancelled_at") val cancelledAt: String? = null,
    @SerialName("in_count") val inCount: Int,
    @SerialName("pending_count") val pendingCount: Int,
    @SerialName("my_status") val myStatus: String? = null
)

@Serializable
internal data class CreateEventDto(
    @SerialName("group_id") val groupId: String,
    val title: String,
    @SerialName("starts_at") val startsAt: String,
    val location: String? = null,
    val capacity: Int
)

@Serializable
internal data class UpdateEventDto(
    @SerialName("event_id") val eventId: String,
    val title: String,
    @SerialName("starts_at") val startsAt: String,
    val location: String? = null
)

@Serializable
internal data class GetEventDetailDto(@SerialName("eid") val eventId: String)

@Serializable
internal data class EventDetailDto(
    val event: EventDto,
    val roster: List<EventRosterEntryDto>,
    val standby: List<EventStandbyEntryDto>,
    @SerialName("my_status") val myStatus: String? = null
)

@Serializable
internal data class EventRosterEntryDto(
    @SerialName("profile_id") val profileId: String,
    val name: String? = null,
    val status: String
)

@Serializable
internal data class SetEventRsvpDto(
    @SerialName("event_id") val eventId: String,
    @SerialName("profile_id") val profileId: String,
    val status: String
)

@Serializable
internal data class EventInvitationGroupIdDto(@SerialName("group_id") val groupId: String)

@Serializable
internal data class EventStandbyEntryDto(
    @SerialName("profile_id") val profileId: String,
    val name: String? = null
)
