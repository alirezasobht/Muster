package app.muster.data.repository

import app.muster.data.dto.GroupInvitationRowDto
import app.muster.data.dto.GroupMemberDto
import app.muster.data.dto.InviteGroupMemberDto
import app.muster.data.dto.LeaveGroupDto
import app.muster.data.dto.RemoveGroupMemberDto
import app.muster.data.dto.RevokeGroupInvitationDto
import app.muster.data.dto.SetGroupMemberRoleDto
import app.muster.data.mapper.mapErrors
import app.muster.data.mapper.toMember
import app.muster.data.mapper.toPendingInvitation
import app.muster.domain.event.DataChange
import app.muster.domain.event.DataChanges
import app.muster.domain.model.MemberListing
import app.muster.domain.repository.MemberRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc

internal class MemberRepositoryImpl(
    private val client: SupabaseClient,
    private val dataChanges: DataChanges
) : MemberRepository {

    override suspend fun listMembers(groupId: String): MemberListing = mapErrors {
        val members = client.from(GROUP_MEMBERS_TABLE)
            .select(Columns.raw("profile_id,role,profiles(name)")) {
                filter { eq("group_id", groupId) }
            }
            .decodeList<GroupMemberDto>()
            .map { it.toMember() }

        val pendingInvitations = client.from(GROUP_INVITATIONS_TABLE)
            .select(Columns.raw("id,email")) {
                filter {
                    eq("group_id", groupId)
                    eq("status", PENDING_STATUS)
                }
            }
            .decodeList<GroupInvitationRowDto>()
            .map { it.toPendingInvitation() }

        MemberListing(members = members, pendingInvitations = pendingInvitations)
    }

    override suspend fun inviteByEmail(groupId: String, email: String): Unit = mapErrors {
        client.postgrest.rpc(
            INVITE_GROUP_MEMBER_BY_EMAIL_FUNCTION,
            InviteGroupMemberDto(groupId = groupId, email = email)
        )
        dataChanges.notify(DataChange.Members(groupId))
    }

    override suspend fun promote(groupId: String, profileId: String): Unit = mapErrors {
        setRole(groupId, profileId, ADMIN_ROLE)
        dataChanges.notify(DataChange.Members(groupId))
    }

    override suspend fun demote(groupId: String, profileId: String): Unit = mapErrors {
        setRole(groupId, profileId, MEMBER_ROLE)
        dataChanges.notify(DataChange.Members(groupId))
    }

    override suspend fun remove(groupId: String, profileId: String): Unit = mapErrors {
        client.postgrest.rpc(
            REMOVE_GROUP_MEMBER_FUNCTION,
            RemoveGroupMemberDto(groupId, profileId)
        )
        dataChanges.notify(DataChange.Members(groupId))
    }

    override suspend fun revokeInvitation(groupId: String, invitationId: String): Unit = mapErrors {
        client.postgrest.rpc(
            REVOKE_GROUP_INVITATION_FUNCTION,
            RevokeGroupInvitationDto(groupId, invitationId)
        )
        dataChanges.notify(DataChange.Members(groupId))
    }

    // MyGroups only: this screen's Members list belongs to a group the caller
    // is no longer in, so refetching it could only fail.
    override suspend fun leave(groupId: String): Unit = mapErrors {
        client.postgrest.rpc(LEAVE_GROUP_FUNCTION, LeaveGroupDto(groupId))
        dataChanges.notify(DataChange.MyGroups)
    }

    private suspend fun setRole(groupId: String, profileId: String, role: String) {
        client.postgrest.rpc(
            SET_GROUP_MEMBER_ROLE_FUNCTION,
            SetGroupMemberRoleDto(groupId = groupId, profileId = profileId, role = role)
        )
    }

    private companion object {
        const val GROUP_MEMBERS_TABLE = "group_members"
        const val GROUP_INVITATIONS_TABLE = "group_invitations"
        const val PENDING_STATUS = "pending"
        const val ADMIN_ROLE = "admin"
        const val MEMBER_ROLE = "member"
        const val INVITE_GROUP_MEMBER_BY_EMAIL_FUNCTION = "invite_group_member_by_email"
        const val SET_GROUP_MEMBER_ROLE_FUNCTION = "set_group_member_role"
        const val REMOVE_GROUP_MEMBER_FUNCTION = "remove_group_member"
        const val REVOKE_GROUP_INVITATION_FUNCTION = "revoke_group_invitation"
        const val LEAVE_GROUP_FUNCTION = "leave_group"
    }
}
