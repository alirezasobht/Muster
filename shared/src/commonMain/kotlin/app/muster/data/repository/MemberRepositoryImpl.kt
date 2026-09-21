package app.muster.data.repository

import app.muster.data.dto.GroupInvitationInsertDto
import app.muster.data.dto.GroupInvitationRowDto
import app.muster.data.dto.GroupMemberDto
import app.muster.data.dto.GroupMemberRoleUpdateDto
import app.muster.data.dto.InviteGroupMemberDto
import app.muster.data.mapper.mapErrors
import app.muster.data.mapper.toMember
import app.muster.data.mapper.toPendingInvitation
import app.muster.domain.error.DomainError
import app.muster.domain.event.DataChange
import app.muster.domain.event.DataChanges
import app.muster.domain.model.MemberListing
import app.muster.domain.repository.MemberRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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

//    override suspend fun inviteByEmail(groupId: String, email: String): Unit = mapErrors {
//        val normalizedEmail = email.trim().lowercase()
//        client.from(GROUP_INVITATIONS_TABLE).insert(
//            GroupInvitationInsertDto(
//                groupId = groupId,
//                email = normalizedEmail,
//                invitedBy = myId()
//            )
//        )
//        dataChanges.notify(DataChange.Members(groupId))
//    }

    override suspend fun inviteByEmail(groupId: String, email: String): Unit = mapErrors {
        client.postgrest.rpc(
            INVITE_GROUP_MEMBER_BY_EMAIL_FUNCTION,
            InviteGroupMemberDto(
                groupId = groupId,
                email = email
            )
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
        client.from(GROUP_MEMBERS_TABLE).delete {
            filter {
                eq("group_id", groupId)
                eq("profile_id", profileId)
            }
        }
        dataChanges.notify(DataChange.Members(groupId))
    }

    override suspend fun revokeInvitation(groupId: String, invitationId: String): Unit = mapErrors {
        client.from(GROUP_INVITATIONS_TABLE).delete {
            filter {
                eq("id", invitationId)
                eq("group_id", groupId)
            }
        }
        dataChanges.notify(DataChange.Members(groupId))
    }

    // Same delete as an admin removing someone else (group_members_delete
    // allows either) — the only difference is whose id ends up in the filter.
    override suspend fun leave(groupId: String): Unit = mapErrors {
        client.from(GROUP_MEMBERS_TABLE).delete {
            filter {
                eq("group_id", groupId)
                eq("profile_id", myId())
            }
        }
        dataChanges.notify(DataChange.Members(groupId))
        dataChanges.notify(DataChange.MyGroups)
    }

    private suspend fun setRole(groupId: String, profileId: String, role: String) {
        client.from(GROUP_MEMBERS_TABLE).update(GroupMemberRoleUpdateDto(role = role)) {
            filter {
                eq("group_id", groupId)
                eq("profile_id", profileId)
            }
        }
    }

    private fun myId(): String =
        client.auth.currentUserOrNull()?.id ?: throw DomainError.NotSignedIn()

    private companion object {
        const val GROUP_MEMBERS_TABLE = "group_members"
        const val GROUP_INVITATIONS_TABLE = "group_invitations"
        const val PENDING_STATUS = "pending"
        const val ADMIN_ROLE = "admin"
        const val MEMBER_ROLE = "member"
        const val INVITE_GROUP_MEMBER_BY_EMAIL_FUNCTION = "invite_group_member_by_email"
    }
}
