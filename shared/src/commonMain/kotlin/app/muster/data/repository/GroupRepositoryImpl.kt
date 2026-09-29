package app.muster.data.repository

import app.muster.data.dto.ArchiveGroupDto
import app.muster.data.dto.CreateGroupDto
import app.muster.data.dto.GroupDto
import app.muster.data.dto.GroupMemberRoleDto
import app.muster.data.dto.GroupMembershipDto
import app.muster.data.dto.PendingInvitationDto
import app.muster.data.mapper.mapErrors
import app.muster.data.mapper.toGroup
import app.muster.data.mapper.toGroupInvitation
import app.muster.data.mapper.toGroupRole
import app.muster.domain.error.DomainError
import app.muster.domain.event.DataChange
import app.muster.domain.event.DataChanges
import app.muster.domain.model.Group
import app.muster.domain.model.GroupInvitation
import app.muster.domain.model.GroupRole
import app.muster.domain.repository.GroupRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class GroupRepositoryImpl(
    private val client: SupabaseClient,
    private val dataChanges: DataChanges
) : GroupRepository {

    override suspend fun getMyGroups(): List<Group> = mapErrors {
        client.from(GROUP_MEMBERS_TABLE)
            .select(Columns.raw("groups(id,name)")) { filter { eq("profile_id", myId()) } }
            .decodeList<GroupMembershipDto>()
            .map { it.toGroup() }
    }

    override suspend fun createGroup(name: String): Group = mapErrors {
        client.postgrest.rpc(CREATE_GROUP_FUNCTION, CreateGroupDto(name))
            .decodeSingle<GroupDto>()
            .toGroup()
            .also { dataChanges.notify(DataChange.MyGroups) }
    }

    override suspend fun getGroup(groupId: String): Group = mapErrors {
        client.from(GROUPS_TABLE)
            .select { filter { eq("id", groupId) } }
            .decodeSingle<GroupDto>()
            .toGroup()
    }

    override suspend fun getMyRole(groupId: String): GroupRole = mapErrors {
        client.from(GROUP_MEMBERS_TABLE)
            .select(Columns.raw("role")) {
                filter {
                    eq("group_id", groupId)
                    eq("profile_id", myId())
                }
            }
            .decodeSingle<GroupMemberRoleDto>()
            .toGroupRole()
    }

    override suspend fun getPendingInvitations(): List<GroupInvitation> = mapErrors {
        client.postgrest.rpc(GET_MY_PENDING_INVITATIONS_FUNCTION)
            .decodeList<PendingInvitationDto>()
            .map { it.toGroupInvitation() }
    }

    override suspend fun acceptInvitation(invitationId: String): Unit = mapErrors {
        client.postgrest.rpc(ACCEPT_INVITATION_FUNCTION, invitationIdParams(invitationId))
        dataChanges.notify(DataChange.MyGroups)
    }

    override suspend fun declineInvitation(invitationId: String): Unit = mapErrors {
        client.postgrest.rpc(DECLINE_INVITATION_FUNCTION, invitationIdParams(invitationId))
    }

    override suspend fun archive(groupId: String): Unit = mapErrors {
        client.postgrest.rpc(ARCHIVE_GROUP_FUNCTION, ArchiveGroupDto(groupId))
        dataChanges.notify(DataChange.MyGroups)
    }

    private fun invitationIdParams(invitationId: String) = buildJsonObject {
        put("invitation_id", invitationId)
    }

    private fun myId(): String = client.auth.currentUserOrNull()?.id ?: throw DomainError.NotSignedIn()

    private companion object {
        const val GROUPS_TABLE = "groups"
        const val GROUP_MEMBERS_TABLE = "group_members"
        const val CREATE_GROUP_FUNCTION = "create_group"
        const val ARCHIVE_GROUP_FUNCTION = "archive_group"
        const val GET_MY_PENDING_INVITATIONS_FUNCTION = "get_my_pending_invitations"
        const val ACCEPT_INVITATION_FUNCTION = "accept_group_invitation"
        const val DECLINE_INVITATION_FUNCTION = "decline_group_invitation"
    }
}
