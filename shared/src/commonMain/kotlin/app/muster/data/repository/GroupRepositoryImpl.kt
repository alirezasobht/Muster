package app.muster.data.repository

import app.muster.data.dto.GroupMembershipDto
import app.muster.data.dto.PendingInvitationDto
import app.muster.data.mapper.mapErrors
import app.muster.data.mapper.toGroup
import app.muster.data.mapper.toGroupInvitation
import app.muster.domain.error.DomainError
import app.muster.domain.model.Group
import app.muster.domain.model.GroupInvitation
import app.muster.domain.repository.GroupRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class GroupRepositoryImpl(private val client: SupabaseClient) : GroupRepository {

    override suspend fun getMyGroups(): List<Group> = mapErrors {
        client.from(GROUP_MEMBERS_TABLE)
            .select(Columns.raw("groups(id,name)")) { filter { eq("profile_id", myId()) } }
            .decodeList<GroupMembershipDto>()
            .map { it.toGroup() }
    }

    override suspend fun getPendingInvitations(): List<GroupInvitation> = mapErrors {
        client.postgrest.rpc(GET_MY_PENDING_INVITATIONS_FUNCTION)
            .decodeList<PendingInvitationDto>()
            .map { it.toGroupInvitation() }
    }

    override suspend fun acceptInvitation(invitationId: String): Unit = mapErrors {
        client.postgrest.rpc(ACCEPT_INVITATION_FUNCTION, invitationIdParams(invitationId))
    }

    override suspend fun declineInvitation(invitationId: String): Unit = mapErrors {
        client.postgrest.rpc(DECLINE_INVITATION_FUNCTION, invitationIdParams(invitationId))
    }

    private fun invitationIdParams(invitationId: String) = buildJsonObject {
        put("invitation_id", invitationId)
    }

    private fun myId(): String =
        client.auth.currentUserOrNull()?.id ?: throw DomainError.NotSignedIn()

    private companion object {
        const val GROUP_MEMBERS_TABLE = "group_members"
        const val GET_MY_PENDING_INVITATIONS_FUNCTION = "get_my_pending_invitations"
        const val ACCEPT_INVITATION_FUNCTION = "accept_group_invitation"
        const val DECLINE_INVITATION_FUNCTION = "decline_group_invitation"
    }
}
