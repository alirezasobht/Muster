package app.muster.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GroupMemberDto(
    @SerialName("profile_id") val profileId: String,
    val role: String,
    val profiles: MemberProfileDto
)

@Serializable
internal data class MemberProfileDto(
    val name: String? = null
)

@Serializable
internal data class GroupInvitationRowDto(
    val id: String,
    val email: String
)

@Serializable
internal data class SetGroupMemberRoleDto(
    @SerialName("group_id") val groupId: String,
    @SerialName("profile_id") val profileId: String,
    val role: String
)

@Serializable
internal data class InviteGroupMemberDto(
    @SerialName("group_id") val groupId: String,
    val email: String
)

@Serializable
internal data class RemoveGroupMemberDto(
    @SerialName("group_id") val groupId: String,
    @SerialName("profile_id") val profileId: String
)

@Serializable
internal data class RevokeGroupInvitationDto(
    @SerialName("group_id") val groupId: String,
    @SerialName("invitation_id") val invitationId: String
)

@Serializable
internal data class LeaveGroupDto(
    @SerialName("group_id") val groupId: String
)
