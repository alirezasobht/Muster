package app.muster.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GetGroupMembersDto(@SerialName("group_id") val groupId: String)

// What get_group_members returns: members list, pending list and tags list.
// A caller who isn't in the group gets no row.
@Serializable
internal data class GroupMembersDto(
    val members: List<GroupMemberEntryDto>,
    val pending: List<GroupInvitationEntryDto>,
    val tags: List<GroupMemberTagEntryDto>
)

@Serializable
internal data class GroupMemberEntryDto(
    @SerialName("profile_id") val profileId: String,
    val name: String? = null,
    val role: String
)

@Serializable
internal data class GroupInvitationEntryDto(
    @SerialName("invitation_id") val invitationId: String,
    val email: String
)

@Serializable
internal data class GroupMemberTagEntryDto(
    val id: String,
    val name: String,
    // Profile ids, already in the tag's order.
    val members: List<String>
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
internal data class ResendGroupInvitationDto(
    @SerialName("group_id") val groupId: String,
    @SerialName("invitation_id") val invitationId: String,
    val tz: String
)

@Serializable
internal data class LeaveGroupDto(@SerialName("group_id") val groupId: String)

@Serializable
internal data class CreateGroupTagDto(
    @SerialName("group_id") val groupId: String,
    val name: String,
    @SerialName("profile_ids") val profileIds: List<String>
)

@Serializable
internal data class UpdateGroupTagDto(
    @SerialName("tag_id") val tagId: String,
    val name: String
)

@Serializable
internal data class DeleteGroupTagDto(@SerialName("tag_id") val tagId: String)

@Serializable
internal data class SetGroupTagLayoutDto(
    @SerialName("group_id") val groupId: String,
    val layout: List<TagLayoutEntryDto>
)

@Serializable
internal data class TagLayoutEntryDto(
    @SerialName("tag_id") val tagId: String,
    @SerialName("profile_ids") val profileIds: List<String>
)

@Serializable
internal data class SetGroupTagOrderDto(
    @SerialName("group_id") val groupId: String,
    @SerialName("tag_ids") val tagIds: List<String>
)
