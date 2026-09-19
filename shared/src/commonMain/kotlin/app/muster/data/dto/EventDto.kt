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

// RLS requires the caller to be a group admin; created_by is the caller.
@Serializable
internal data class EventInsertDto(
    @SerialName("group_id") val groupId: String,
    val title: String,
    @SerialName("starts_at") val startsAt: String,
    val location: String? = null,
    val capacity: Int,
    @SerialName("created_by") val createdBy: String
)

@Serializable
internal data class EventInvitationRowDto(
    @SerialName("event_id") val eventId: String,
    @SerialName("profile_id") val profileId: String,
    val status: String
)

@Serializable
internal data class EventRsvpUpdateDto(
    val status: String
)

@Serializable
internal data class EventInvitationGroupIdDto(
    @SerialName("group_id") val groupId: String
)
