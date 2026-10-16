package app.muster.data.repository

import app.muster.data.dto.CreateGroupTagDto
import app.muster.data.dto.DeleteGroupTagDto
import app.muster.data.dto.GetGroupMembersDto
import app.muster.data.dto.GroupMembersDto
import app.muster.data.dto.InviteGroupMemberDto
import app.muster.data.dto.LeaveGroupDto
import app.muster.data.dto.RemoveGroupMemberDto
import app.muster.data.dto.ResendGroupInvitationDto
import app.muster.data.dto.RevokeGroupInvitationDto
import app.muster.data.dto.SetGroupMemberRoleDto
import app.muster.data.dto.SetGroupTagLayoutDto
import app.muster.data.dto.SetGroupTagOrderDto
import app.muster.data.dto.UpdateGroupTagDto
import app.muster.data.mapper.mapErrors
import app.muster.data.mapper.toDto
import app.muster.data.mapper.toMemberListing
import app.muster.domain.error.DomainError
import app.muster.domain.event.DataChange
import app.muster.domain.event.DataChanges
import app.muster.domain.model.MemberListing
import app.muster.domain.model.MusterTimeZone
import app.muster.domain.model.TagLayoutEntry
import app.muster.domain.repository.MemberRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc

internal class MemberRepositoryImpl(
    private val client: SupabaseClient,
    private val dataChanges: DataChanges
) : MemberRepository {

    // No row means the caller isn't in the group
    override suspend fun listMembers(groupId: String): MemberListing = mapErrors {
        client.postgrest.rpc(GET_GROUP_MEMBERS_FUNCTION, GetGroupMembersDto(groupId))
            .decodeList<GroupMembersDto>()
            .firstOrNull()
            ?.toMemberListing()
            ?: MemberListing(members = emptyList(), pendingInvitations = emptyList())
    }

    override suspend fun inviteByEmail(
        groupId: String,
        email: String
    ): Unit = mapErrors {
        client.postgrest.rpc(
            INVITE_GROUP_MEMBER_BY_EMAIL_FUNCTION,
            InviteGroupMemberDto(groupId = groupId, email = email)
        )
        dataChanges.notify(DataChange.Members(groupId))
    }

    override suspend fun promote(
        groupId: String,
        profileId: String
    ): Unit = mapErrors {
        setRole(groupId, profileId, ADMIN_ROLE)
        dataChanges.notify(DataChange.Members(groupId))
    }

    override suspend fun demote(
        groupId: String,
        profileId: String
    ): Unit = mapErrors {
        setRole(groupId, profileId, MEMBER_ROLE)
        dataChanges.notify(DataChange.Members(groupId))
    }

    override suspend fun remove(
        groupId: String,
        profileId: String
    ): Unit = mapErrors {
        client.postgrest.rpc(
            REMOVE_GROUP_MEMBER_FUNCTION,
            RemoveGroupMemberDto(groupId, profileId)
        )
        dataChanges.notify(DataChange.Members(groupId))
    }

    override suspend fun revokeInvitation(
        groupId: String,
        invitationId: String
    ): Unit = mapErrors {
        client.postgrest.rpc(
            REVOKE_GROUP_INVITATION_FUNCTION,
            RevokeGroupInvitationDto(groupId, invitationId)
        )
        dataChanges.notify(DataChange.Members(groupId))
    }

    override suspend fun resendInvitation(
        groupId: String,
        invitationId: String
    ): Unit = mapErrors {
        client.postgrest.rpc(
            RESEND_GROUP_INVITATION_FUNCTION,
            ResendGroupInvitationDto(groupId, invitationId, tz = MusterTimeZone.id)
        )
    }

    override suspend fun createTag(
        groupId: String,
        name: String,
        memberIds: List<String>
    ): Unit = mapErrors {
        namingTag(name) {
            client.postgrest.rpc(CREATE_GROUP_TAG_FUNCTION, CreateGroupTagDto(groupId, name, memberIds))
        }
        dataChanges.notify(DataChange.Members(groupId))
    }

    override suspend fun renameTag(
        groupId: String,
        tagId: String,
        name: String
    ): Unit = mapErrors {
        namingTag(name) {
            client.postgrest.rpc(UPDATE_GROUP_TAG_FUNCTION, UpdateGroupTagDto(tagId, name))
        }
        dataChanges.notify(DataChange.Members(groupId))
    }

    override suspend fun deleteTag(
        groupId: String,
        tagId: String
    ): Unit = mapErrors {
        client.postgrest.rpc(DELETE_GROUP_TAG_FUNCTION, DeleteGroupTagDto(tagId))
        dataChanges.notify(DataChange.Members(groupId))
    }

    override suspend fun setTagLayout(
        groupId: String,
        layout: List<TagLayoutEntry>
    ): Unit = mapErrors {
        client.postgrest.rpc(
            SET_GROUP_TAG_LAYOUT_FUNCTION,
            SetGroupTagLayoutDto(groupId, layout.map { it.toDto() })
        )
        dataChanges.notify(DataChange.Members(groupId))
    }

    override suspend fun setTagOrder(
        groupId: String,
        tagIds: List<String>
    ): Unit = mapErrors {
        client.postgrest.rpc(SET_GROUP_TAG_ORDER_FUNCTION, SetGroupTagOrderDto(groupId, tagIds))
        dataChanges.notify(DataChange.Members(groupId))
    }

    // MyGroups only: this screen's Members list belongs to a group the caller
    // is no longer in, so refetching it could only fail.
    override suspend fun leave(groupId: String): Unit = mapErrors {
        client.postgrest.rpc(LEAVE_GROUP_FUNCTION, LeaveGroupDto(groupId))
        dataChanges.notify(DataChange.MyGroups)
    }

    private suspend fun setRole(
        groupId: String,
        profileId: String,
        role: String
    ) {
        client.postgrest.rpc(
            SET_GROUP_MEMBER_ROLE_FUNCTION,
            SetGroupMemberRoleDto(groupId = groupId, profileId = profileId, role = role)
        )
    }

    // The unique index doesn't say which name collided, and the message quotes it.
    private suspend fun namingTag(
        name: String,
        call: suspend () -> Unit
    ) {
        try {
            call()
        } catch (e: PostgrestRestException) {
            if (e.code == UNIQUE_VIOLATION && TAG_NAME_UNIQUE_INDEX in e.error) throw DomainError.TagNameTaken(name)
            throw e
        }
    }

    private companion object {
        const val UNIQUE_VIOLATION = "23505"
        const val TAG_NAME_UNIQUE_INDEX = "group_tags_name_unique"
        const val ADMIN_ROLE = "admin"
        const val MEMBER_ROLE = "member"
        const val GET_GROUP_MEMBERS_FUNCTION = "get_group_members"
        const val CREATE_GROUP_TAG_FUNCTION = "create_group_tag"
        const val UPDATE_GROUP_TAG_FUNCTION = "update_group_tag"
        const val DELETE_GROUP_TAG_FUNCTION = "delete_group_tag"
        const val SET_GROUP_TAG_LAYOUT_FUNCTION = "set_group_tag_layout"
        const val SET_GROUP_TAG_ORDER_FUNCTION = "set_group_tag_order"
        const val INVITE_GROUP_MEMBER_BY_EMAIL_FUNCTION = "invite_group_member_by_email"
        const val SET_GROUP_MEMBER_ROLE_FUNCTION = "set_group_member_role"
        const val REMOVE_GROUP_MEMBER_FUNCTION = "remove_group_member"
        const val REVOKE_GROUP_INVITATION_FUNCTION = "revoke_group_invitation"
        const val RESEND_GROUP_INVITATION_FUNCTION = "resend_group_invitation"
        const val LEAVE_GROUP_FUNCTION = "leave_group"
    }
}
