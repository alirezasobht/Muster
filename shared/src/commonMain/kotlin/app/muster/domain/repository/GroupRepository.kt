package app.muster.domain.repository

import app.muster.domain.model.Group
import app.muster.domain.model.GroupInvitation
import app.muster.domain.model.GroupRole

// Implementations must throw DomainError only; the UI catches nothing else.
interface GroupRepository {

    suspend fun getMyGroups(): List<Group>

    suspend fun createGroup(name: String): Group

    suspend fun getGroup(groupId: String): Group

    suspend fun getMyRole(groupId: String): GroupRole

    suspend fun getPendingInvitations(): List<GroupInvitation>

    suspend fun acceptInvitation(invitationId: String)

    suspend fun declineInvitation(invitationId: String)
}
