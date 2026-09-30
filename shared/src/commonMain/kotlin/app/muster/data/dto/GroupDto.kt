package app.muster.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GroupDto(
    val id: String,
    val name: String
)

@Serializable
internal data class CreateGroupDto(val name: String)

@Serializable
internal data class ArchiveGroupDto(@SerialName("group_id") val groupId: String)

// group_members joined to groups, for "my groups".
@Serializable
internal data class GroupMembershipDto(val groups: GroupDto)

// The caller's own group_members row, for their role in one group.
@Serializable
internal data class GroupMemberRoleDto(val role: String)

// get_my_pending_invitations() RPC result — flat, not a table select.
@Serializable
internal data class PendingInvitationDto(
    @SerialName("invitation_id") val invitationId: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("group_name") val groupName: String,
    @SerialName("inviter_name") val inviterName: String? = null
)
