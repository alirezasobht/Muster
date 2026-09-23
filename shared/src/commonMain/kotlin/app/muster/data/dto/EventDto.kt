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
    val capacity: Int
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
internal data class EventInvitationRowDto(
    @SerialName("event_id") val eventId: String,
    @SerialName("profile_id") val profileId: String,
    val status: String
)

@Serializable
internal data class SetEventRsvpDto(
    @SerialName("event_id") val eventId: String,
    @SerialName("profile_id") val profileId: String,
    val status: String
)

@Serializable
internal data class EventInvitationGroupIdDto(
    @SerialName("group_id") val groupId: String
)

@Serializable
internal data class EventStandbyRowDto(
    @SerialName("profile_id") val profileId: String
)
