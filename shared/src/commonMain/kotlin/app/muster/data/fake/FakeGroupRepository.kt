package app.muster.data.fake

import app.muster.domain.error.DomainError
import app.muster.domain.model.Group
import app.muster.domain.model.GroupInvitation
import app.muster.domain.repository.GroupRepository
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class FakeGroupRepository(
    groups: List<Group> = emptyList(),
    invitations: List<GroupInvitation> = emptyList(),
    var getMyGroupsError: DomainError? = null,
    var getPendingInvitationsError: DomainError? = null,
    var acceptError: DomainError? = null,
    var declineError: DomainError? = null,
    private val latency: Long = FAKE_LATENCY_MS
) : GroupRepository {

    var groups = groups
        private set

    var invitations = invitations
        private set

    var acceptedInvitationIds = listOf<String>()
        private set

    var declinedInvitationIds = listOf<String>()
        private set

    override suspend fun getMyGroups(): List<Group> {
        delay(latency.milliseconds)
        getMyGroupsError?.let { throw it }
        return groups
    }

    override suspend fun getPendingInvitations(): List<GroupInvitation> {
        delay(latency.milliseconds)
        getPendingInvitationsError?.let { throw it }
        return invitations
    }

    override suspend fun acceptInvitation(invitationId: String) {
        delay(latency.milliseconds)
        acceptError?.let { throw it }
        acceptedInvitationIds = acceptedInvitationIds + invitationId
        invitations = invitations.filterNot { it.id == invitationId }
    }

    override suspend fun declineInvitation(invitationId: String) {
        delay(latency.milliseconds)
        declineError?.let { throw it }
        declinedInvitationIds = declinedInvitationIds + invitationId
        invitations = invitations.filterNot { it.id == invitationId }
    }
}
