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
internal data class GroupMemberRoleUpdateDto(
    val role: String
)

@Serializable
internal data class GroupInvitationRowDto(
    val id: String,
    val email: String
)

@Serializable
internal data class GroupInvitationInsertDto(
    @SerialName("group_id") val groupId: String,
    val email: String,
    @SerialName("invited_by") val invitedBy: String,
    val status: String = "pending"
)

@Serializable
data class InviteGroupMemberDto(
    @SerialName("group_id")
    val groupId: String,

    val email: String
)
