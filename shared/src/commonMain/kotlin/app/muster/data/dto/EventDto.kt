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

// A second query against event_invitations, grouped client-side by event_id
// to get in/pending counts and the caller's own status — the event row and
// the roster are different tables, same as MemberListing's two queries.
@Serializable
internal data class EventInvitationRowDto(
    @SerialName("event_id") val eventId: String,
    @SerialName("profile_id") val profileId: String,
    val status: String
)
