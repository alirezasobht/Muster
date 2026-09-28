package app.muster.ui.screens.group.members

import app.muster.data.fake.FAKE_USER_ID
import app.muster.data.fake.FakeGroupRepository
import app.muster.data.fake.FakeMemberRepository
import app.muster.data.fake.FakeProfileRepository
import app.muster.domain.error.DomainError
import app.muster.domain.event.DataChanges
import app.muster.domain.model.GroupRole
import app.muster.domain.model.Member
import app.muster.domain.model.PendingInvitation
import app.muster.domain.usecase.DemoteMemberUseCase
import app.muster.domain.usecase.GetMyGroupRoleUseCase
import app.muster.domain.usecase.GetMyProfileUseCase
import app.muster.domain.usecase.ListGroupMembersUseCase
import app.muster.domain.usecase.PromoteMemberUseCase
import app.muster.domain.usecase.RemoveMemberUseCase
import app.muster.domain.usecase.ResendInvitationUseCase
import app.muster.domain.usecase.RevokeInvitationUseCase
import app.muster.testing.MainDispatcherRule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class MembersViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val groupId = "group-1"

    // FAKE_USER_ID is what FakeProfileRepository/getMyProfile() reports, so
    // giving a member that same profileId is how "self" gets represented.
    private val self = Member(profileId = FAKE_USER_ID, name = "Alex Doyle", role = GroupRole.Admin)
    private val otherAdmin = Member(profileId = "admin-2", name = "Dan Whelan", role = GroupRole.Admin)
    private val plainMember = Member(profileId = "member-1", name = "Sam Okafor", role = GroupRole.Member)
    private val pending = PendingInvitation(id = "invite-1", email = "j.moriarty@outlook.com")

    // The fake and the ViewModel must share one instance: actions no longer
    // refetch inline, they rely on the repository's announcement.
    private fun viewModel(
        groupId: String = this.groupId,
        profiles: FakeProfileRepository = FakeProfileRepository(),
        groups: FakeGroupRepository = FakeGroupRepository(myRole = GroupRole.Admin),
        members: FakeMemberRepository = FakeMemberRepository(),
        dataChanges: DataChanges = DataChanges()
    ) = MembersViewModel(
        groupId = groupId,
        listGroupMembers = ListGroupMembersUseCase(members),
        getMyGroupRole = GetMyGroupRoleUseCase(groups),
        getMyProfile = GetMyProfileUseCase(profiles),
        promoteMember = PromoteMemberUseCase(members),
        demoteMember = DemoteMemberUseCase(members),
        removeMember = RemoveMemberUseCase(members),
        revokeInvitation = RevokeInvitationUseCase(members),
        resendInvitation = ResendInvitationUseCase(members),
        dataChanges = dataChanges
    )

    @Test
    fun `starts loading`() {
        assertEquals(MembersUiState.Loading, viewModel().state.value)
    }

    @Test
    fun `an admin viewer sees members and pending invitations with action flags set`() = runTest {
        val groups = FakeGroupRepository(myRole = GroupRole.Admin)
        val members = FakeMemberRepository(
            members = listOf(self, otherAdmin, plainMember),
            pendingInvitations = listOf(pending)
        )
        val viewModel = viewModel(groups = groups, members = members)
        advanceUntilIdle()

        val state = assertIs<MembersUiState.Success>(viewModel.state.value)
        assertEquals(true, state.canAddMembers)
        assertEquals(4, state.rows.size)

        val selfRow = state.rows.first { it.id == FAKE_USER_ID }
        assertEquals(true, selfRow.isSelf)
        assertEquals(false, selfRow.hasMenu)

        val otherAdminRow = state.rows.first { it.id == otherAdmin.profileId }
        assertEquals(true, otherAdminRow.canDemote)
        assertEquals(true, otherAdminRow.canRemove)
        assertEquals(false, otherAdminRow.canPromote)

        val memberRow = state.rows.first { it.id == plainMember.profileId }
        assertEquals(true, memberRow.canPromote)
        assertEquals(true, memberRow.canRemove)
        assertEquals(false, memberRow.canDemote)

        val pendingRow = state.rows.first { it.id == pending.id }
        assertEquals(MemberStatus.Pending, pendingRow.status)
        assertEquals(pending.email, pendingRow.displayName)
        assertEquals(true, pendingRow.canRevokeInvitation)
        assertEquals(true, pendingRow.canResendInvitation)
    }

    @Test
    fun `rows are ordered self first, then admins, then members, then pending, each A-Z`() = runTest {
        val zedAdmin = Member(profileId = "admin-zed", name = "Zed Admin", role = GroupRole.Admin)
        val annAdmin = Member(profileId = "admin-ann", name = "Ann Admin", role = GroupRole.Admin)
        val zedMember = Member(profileId = "member-zed", name = "Zed Member", role = GroupRole.Member)
        val annMember = Member(profileId = "member-ann", name = "Ann Member", role = GroupRole.Member)
        val zetaInvite = PendingInvitation(id = "invite-zeta", email = "zeta@example.com")
        val alphaInvite = PendingInvitation(id = "invite-alpha", email = "alpha@example.com")

        // Deliberately scrambled input, so a pass-through of DB order would
        // fail this test rather than accidentally satisfy it.
        val members = FakeMemberRepository(
            members = listOf(zedMember, zedAdmin, self, annMember, annAdmin),
            pendingInvitations = listOf(zetaInvite, alphaInvite)
        )
        val viewModel = viewModel(members = members)
        advanceUntilIdle()

        val state = assertIs<MembersUiState.Success>(viewModel.state.value)
        assertEquals(
            listOf(
                FAKE_USER_ID,
                "admin-ann",
                "admin-zed",
                "member-ann",
                "member-zed",
                "invite-alpha",
                "invite-zeta"
            ),
            state.rows.map { it.id }
        )
    }

    @Test
    fun `a member viewer never sees pending rows or action flags`() = runTest {
        val groups = FakeGroupRepository(myRole = GroupRole.Member)
        val members = FakeMemberRepository(
            members = listOf(otherAdmin, plainMember.copy(profileId = FAKE_USER_ID)),
            pendingInvitations = listOf(pending)
        )
        val viewModel = viewModel(groups = groups, members = members)
        advanceUntilIdle()

        val state = assertIs<MembersUiState.Success>(viewModel.state.value)
        assertEquals(false, state.canAddMembers)
        assertEquals(2, state.rows.size)
        assertEquals(true, state.rows.none { it.status == MemberStatus.Pending })
        assertEquals(true, state.rows.none { it.hasMenu })
    }

    @Test
    fun `a members load failure reports Error`() = runTest {
        val members = FakeMemberRepository(listMembersError = DomainError.Network())
        val viewModel = viewModel(members = members)
        advanceUntilIdle()

        assertIs<MembersUiState.Error>(viewModel.state.value)
    }

    @Test
    fun `retrying after a failed load can reach Success`() = runTest {
        val members = FakeMemberRepository(listMembersError = DomainError.Network())
        val viewModel = viewModel(members = members)
        advanceUntilIdle()
        assertIs<MembersUiState.Error>(viewModel.state.value)

        members.listMembersError = null
        viewModel.onRetry()
        advanceUntilIdle()

        assertIs<MembersUiState.Success>(viewModel.state.value)
    }

    @Test
    fun `promoting a member calls the repository once and the row reflects it`() = runTest {
        val changes = DataChanges()
        val members = FakeMemberRepository(members = listOf(self, plainMember), dataChanges = changes)
        val viewModel = viewModel(members = members, dataChanges = changes)
        advanceUntilIdle()

        viewModel.onPromote(plainMember.profileId)
        advanceUntilIdle()

        assertEquals(listOf(plainMember.profileId), members.promotedIds)
        val state = assertIs<MembersUiState.Success>(viewModel.state.value)
        assertEquals(MemberStatus.Admin, state.rows.first { it.id == plainMember.profileId }.status)
        assertNull(state.actionTargetId)
    }

    @Test
    fun `a second action while one is in flight is ignored`() = runTest {
        val members = FakeMemberRepository(members = listOf(self, plainMember))
        val viewModel = viewModel(members = members)
        advanceUntilIdle()

        viewModel.onPromote(plainMember.profileId)
        viewModel.onPromote(plainMember.profileId)
        advanceUntilIdle()

        assertEquals(listOf(plainMember.profileId), members.promotedIds)
    }

    @Test
    fun `a failed action clears the in-flight flag, reports the error and keeps the row`() = runTest {
        val members = FakeMemberRepository(
            members = listOf(self, otherAdmin),
            demoteError = DomainError.LastAdmin()
        )
        val viewModel = viewModel(members = members)
        advanceUntilIdle()

        viewModel.onDemote(otherAdmin.profileId)
        assertEquals(otherAdmin.profileId, (viewModel.state.value as MembersUiState.Success).actionTargetId)
        advanceUntilIdle()

        val state = assertIs<MembersUiState.Success>(viewModel.state.value)
        assertNull(state.actionTargetId)
        assertIs<DomainError.LastAdmin>(state.actionError)
        assertEquals(otherAdmin.profileId, state.failedActionId)
        assertEquals(MemberStatus.Admin, state.rows.first { it.id == otherAdmin.profileId }.status)
    }

    @Test
    fun `removing a member drops their row`() = runTest {
        val changes = DataChanges()
        val members = FakeMemberRepository(members = listOf(self, plainMember), dataChanges = changes)
        val viewModel = viewModel(members = members, dataChanges = changes)
        advanceUntilIdle()

        viewModel.onRemove(plainMember.profileId)
        advanceUntilIdle()

        assertEquals(listOf(plainMember.profileId), members.removedIds)
        val state = assertIs<MembersUiState.Success>(viewModel.state.value)
        assertEquals(true, state.rows.none { it.id == plainMember.profileId })
    }

    @Test
    fun `revoking an invitation drops its row`() = runTest {
        val changes = DataChanges()
        val members = FakeMemberRepository(
            members = listOf(self),
            pendingInvitations = listOf(pending),
            dataChanges = changes
        )
        val viewModel = viewModel(members = members, dataChanges = changes)
        advanceUntilIdle()

        viewModel.onRevokeInvitation(pending.id)
        advanceUntilIdle()

        assertEquals(listOf(pending.id), members.revokedInvitationIds)
        val state = assertIs<MembersUiState.Success>(viewModel.state.value)
        assertEquals(true, state.rows.none { it.id == pending.id })
    }

    @Test
    fun `resending an invitation calls through and keeps its row`() = runTest {
        val members = FakeMemberRepository(members = listOf(self), pendingInvitations = listOf(pending))
        val viewModel = viewModel(members = members)
        advanceUntilIdle()

        viewModel.onResendInvitation(pending.id)
        advanceUntilIdle()

        assertEquals(listOf(pending.id), members.resentInvitationIds)
        val state = assertIs<MembersUiState.Success>(viewModel.state.value)
        assertNull(state.actionError)
        assertEquals(true, state.rows.any { it.id == pending.id })
    }

    @Test
    fun `resending too soon surfaces the throttle error on the row`() = runTest {
        val members = FakeMemberRepository(
            members = listOf(self),
            pendingInvitations = listOf(pending),
            resendError = DomainError.InvitationSentTooRecently()
        )
        val viewModel = viewModel(members = members)
        advanceUntilIdle()

        viewModel.onResendInvitation(pending.id)
        advanceUntilIdle()

        val state = assertIs<MembersUiState.Success>(viewModel.state.value)
        assertIs<DomainError.InvitationSentTooRecently>(state.actionError)
        assertEquals(pending.id, state.failedActionId)
    }

    @Test
    fun `refresh is a no-op before the first load succeeds`() = runTest {
        val members = FakeMemberRepository(listMembersError = DomainError.Network())
        val viewModel = viewModel(members = members)
        advanceUntilIdle()
        assertIs<MembersUiState.Error>(viewModel.state.value)

        viewModel.onRefresh()
        advanceUntilIdle()

        assertIs<MembersUiState.Error>(viewModel.state.value)
    }

    @Test
    fun `the first resume after load is ignored, a later one refreshes`() = runTest {
        val members = FakeMemberRepository(members = listOf(self, plainMember))
        val viewModel = viewModel(members = members)
        advanceUntilIdle()

        viewModel.onResume()
        advanceUntilIdle()
        assertEquals(GroupRole.Member, plainMember.role)

        // Simulates another admin promoting them elsewhere between resumes.
        members.promote(groupId, plainMember.profileId)
        viewModel.onResume()
        advanceUntilIdle()

        val state = assertIs<MembersUiState.Success>(viewModel.state.value)
        assertEquals(MemberStatus.Admin, state.rows.first { it.id == plainMember.profileId }.status)
    }
}
