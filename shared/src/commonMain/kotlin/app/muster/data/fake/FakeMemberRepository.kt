package app.muster.data.fake

import app.muster.domain.error.DomainError
import app.muster.domain.event.DataChange
import app.muster.domain.event.DataChanges
import app.muster.domain.model.GroupRole
import app.muster.domain.model.Member
import app.muster.domain.model.MemberListing
import app.muster.domain.model.PendingInvitation
import app.muster.domain.repository.MemberRepository
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

class FakeMemberRepository(
    members: List<Member> = emptyList(),
    pendingInvitations: List<PendingInvitation> = emptyList(),
    var listMembersError: DomainError? = null,
    var inviteByEmailError: DomainError? = null,
    var promoteError: DomainError? = null,
    var demoteError: DomainError? = null,
    var removeError: DomainError? = null,
    var revokeError: DomainError? = null,
    var resendError: DomainError? = null,
    var leaveError: DomainError? = null,
    private val dataChanges: DataChanges? = null,
    private val latency: Long = FAKE_LATENCY_MS
) : MemberRepository {

    var members = members
        private set

    var pendingInvitations = pendingInvitations
        private set

    var invitedEmails = listOf<String>()
        private set

    var promotedIds = listOf<String>()
        private set

    var demotedIds = listOf<String>()
        private set

    var removedIds = listOf<String>()
        private set

    var revokedInvitationIds = listOf<String>()
        private set

    var resentInvitationIds = listOf<String>()
        private set

    var leftGroupIds = listOf<String>()
        private set

    override suspend fun listMembers(groupId: String): MemberListing {
        delay(latency.milliseconds)
        listMembersError?.let { throw it }
        return MemberListing(members = members, pendingInvitations = pendingInvitations)
    }

    override suspend fun inviteByEmail(
        groupId: String,
        email: String
    ) {
        delay(latency.milliseconds)
        inviteByEmailError?.let { throw it }
        invitedEmails = invitedEmails + email
        dataChanges?.notify(DataChange.Members(groupId))
    }

    override suspend fun promote(
        groupId: String,
        profileId: String
    ) {
        delay(latency.milliseconds)
        promoteError?.let { throw it }
        members = members.map { if (it.profileId == profileId) it.copy(role = GroupRole.Admin) else it }
        promotedIds = promotedIds + profileId
        dataChanges?.notify(DataChange.Members(groupId))
    }

    override suspend fun demote(
        groupId: String,
        profileId: String
    ) {
        delay(latency.milliseconds)
        demoteError?.let { throw it }
        members = members.map { if (it.profileId == profileId) it.copy(role = GroupRole.Member) else it }
        demotedIds = demotedIds + profileId
        dataChanges?.notify(DataChange.Members(groupId))
    }

    override suspend fun remove(
        groupId: String,
        profileId: String
    ) {
        delay(latency.milliseconds)
        removeError?.let { throw it }
        members = members.filterNot { it.profileId == profileId }
        removedIds = removedIds + profileId
        dataChanges?.notify(DataChange.Members(groupId))
    }

    override suspend fun revokeInvitation(
        groupId: String,
        invitationId: String
    ) {
        delay(latency.milliseconds)
        revokeError?.let { throw it }
        pendingInvitations = pendingInvitations.filterNot { it.id == invitationId }
        revokedInvitationIds = revokedInvitationIds + invitationId
        dataChanges?.notify(DataChange.Members(groupId))
    }

    override suspend fun resendInvitation(
        groupId: String,
        invitationId: String
    ) {
        delay(latency.milliseconds)
        resendError?.let { throw it }
        resentInvitationIds = resentInvitationIds + invitationId
    }

    override suspend fun leave(groupId: String) {
        delay(latency.milliseconds)
        leaveError?.let { throw it }
        leftGroupIds = leftGroupIds + groupId
        dataChanges?.notify(DataChange.MyGroups)
    }
}
