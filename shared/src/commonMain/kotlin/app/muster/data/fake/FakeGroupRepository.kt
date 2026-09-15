package app.muster.data.fake

import app.muster.domain.error.DomainError
import app.muster.domain.event.DataChange
import app.muster.domain.event.DataChanges
import app.muster.domain.model.Group
import app.muster.domain.model.GroupInvitation
import app.muster.domain.repository.GroupRepository
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class FakeGroupRepository(
    groups: List<Group> = emptyList(),
    invitations: List<GroupInvitation> = emptyList(),
    var getMyGroupsError: DomainError? = null,
    var createGroupError: DomainError? = null,
    var getPendingInvitationsError: DomainError? = null,
    var acceptError: DomainError? = null,
    var declineError: DomainError? = null,
    private val dataChanges: DataChanges? = null,
    private val latency: Long = FAKE_LATENCY_MS
) : GroupRepository {

    var groups = groups
        private set

    var invitations = invitations
        private set

    var createdGroupNames = listOf<String>()
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

    override suspend fun createGroup(name: String): Group {
        delay(latency.milliseconds)
        createGroupError?.let { throw it }
        val group = Group(id = "fake-group-${groups.size + 1}", name = name)
        groups = groups + group
        createdGroupNames = createdGroupNames + name
        dataChanges?.notify(DataChange.MyGroups)
        return group
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
        dataChanges?.notify(DataChange.MyGroups)
    }

    override suspend fun declineInvitation(invitationId: String) {
        delay(latency.milliseconds)
        declineError?.let { throw it }
        declinedInvitationIds = declinedInvitationIds + invitationId
        invitations = invitations.filterNot { it.id == invitationId }
    }
}
