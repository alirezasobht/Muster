package app.muster.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GroupDto(
    val id: String,
    val name: String
)

// The admin membership row is written by a trigger in the same transaction —
// this is the only write the client makes.
@Serializable
internal data class GroupInsertDto(
    val name: String,
    @SerialName("created_by") val createdBy: String
)

// group_members joined to groups, for "my groups".
@Serializable
internal data class GroupMembershipDto(
    val groups: GroupDto
)

// get_my_pending_invitations() RPC result — flat, not a table select.
@Serializable
internal data class PendingInvitationDto(
    @SerialName("invitation_id") val invitationId: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("group_name") val groupName: String,
    @SerialName("inviter_name") val inviterName: String? = null
)
