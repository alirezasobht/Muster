package app.muster.domain.repository

import app.muster.domain.model.Group
import app.muster.domain.model.GroupInvitation

// Implementations must throw DomainError only; the UI catches nothing else.
interface GroupRepository {

    suspend fun getMyGroups(): List<Group>

    suspend fun getPendingInvitations(): List<GroupInvitation>

    suspend fun acceptInvitation(invitationId: String)

    suspend fun declineInvitation(invitationId: String)
}
