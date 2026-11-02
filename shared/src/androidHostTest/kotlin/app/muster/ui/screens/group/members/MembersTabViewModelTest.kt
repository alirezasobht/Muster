package app.muster.ui.screens.group.members

import app.muster.data.fake.FAKE_USER_ID
import app.muster.data.fake.FakeGroupRepository
import app.muster.data.fake.FakeMemberRepository
import app.muster.data.fake.FakeProfileRepository
import app.muster.domain.error.DomainError
import app.muster.domain.event.DataChanges
import app.muster.domain.model.GroupRole
import app.muster.domain.model.Member
import app.muster.domain.model.MemberTag
import app.muster.domain.model.PendingInvitation
import app.muster.domain.usecase.DeleteTagUseCase
import app.muster.domain.usecase.DemoteMemberUseCase
import app.muster.domain.usecase.GetMyGroupRoleUseCase
import app.muster.domain.usecase.GetMyProfileUseCase
import app.muster.domain.usecase.ListGroupMembersUseCase
import app.muster.domain.usecase.MoveTagDownUseCase
import app.muster.domain.usecase.MoveTagUpUseCase
import app.muster.domain.usecase.PromoteMemberUseCase
import app.muster.domain.usecase.RemoveMemberUseCase
import app.muster.domain.usecase.ResendInvitationUseCase
import app.muster.domain.usecase.RevokeInvitationUseCase
import app.muster.domain.usecase.SetMemberTagUseCase
import app.muster.testing.MainDispatcherRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class MembersTabViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val groupId = "group-1"

    // FAKE_USER_ID is what FakeProfileRepository/getMyProfile() reports, so
    // giving a member that same profileId is how "self" gets represented.
    private val self = Member(profileId = FAKE_USER_ID, name = "Alex Doyle", role = GroupRole.Admin, tagId = null)
    private val otherAdmin = Member(profileId = "admin-2", name = "Dan Whelan", role = GroupRole.Admin, tagId = null)
    private val plainMember = Member(profileId = "member-1", name = "Sam Okafor", role = GroupRole.Member, tagId = null)
    private val pending = PendingInvitation(id = "invite-1", email = "j.moriarty@outlook.com")

    // The fake and the ViewModel must share one instance: actions no longer
    // refetch inline, they rely on the repository's announcement.
    private fun viewModel(
        groupId: String = this.groupId,
        profiles: FakeProfileRepository = FakeProfileRepository(),
        groups: FakeGroupRepository = FakeGroupRepository(myRole = GroupRole.Admin),
        members: FakeMemberRepository = FakeMemberRepository(),
        dataChanges: DataChanges = DataChanges()
    ) = MembersTabViewModel(
        groupId = groupId,
        listGroupMembers = ListGroupMembersUseCase(members),
        getMyGroupRole = GetMyGroupRoleUseCase(groups),
        getMyProfile = GetMyProfileUseCase(profiles),
        promoteMember = PromoteMemberUseCase(members),
        demoteMember = DemoteMemberUseCase(members),
        removeMember = RemoveMemberUseCase(members),
        revokeInvitation = RevokeInvitationUseCase(members),
        resendInvitation = ResendInvitationUseCase(members),
        deleteTag = DeleteTagUseCase(members),
        setMemberTag = SetMemberTagUseCase(members),
        moveTagUp = MoveTagUpUseCase(members),
        moveTagDown = MoveTagDownUseCase(members),
        dataChanges = dataChanges
    )

    private val defenders = MemberTag(id = "tag-def", name = "Defenders")
    private val forwards = MemberTag(id = "tag-fwd", name = "Forwards")
    private val taggedMember = plainMember.copy(tagId = defenders.id)

    private fun MembersTabViewModel.success() = assertIs<MembersTabUiState.Success>(state.value)

    private fun MembersTabUiState.Success.allRows() = tags.flatMap { it.rows } + members + pendings

    @Test
    fun `starts loading`() {
        assertEquals(MembersTabUiState.Loading, viewModel().state.value)
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

        val state = assertIs<MembersTabUiState.Success>(viewModel.state.value)
        assertEquals(true, state.canAddMembers)
        assertEquals(4, state.allRows().size)

        val selfRow = state.allRows().first { it.id == FAKE_USER_ID }
        assertEquals(true, selfRow.isSelf)
        assertEquals(false, selfRow.hasRoleActions)

        val otherAdminRow = state.allRows().first { it.id == otherAdmin.profileId }
        assertEquals(true, otherAdminRow.canDemote)
        assertEquals(true, otherAdminRow.canRemove)
        assertEquals(false, otherAdminRow.canPromote)

        val memberRow = state.allRows().first { it.id == plainMember.profileId }
        assertEquals(true, memberRow.canPromote)
        assertEquals(true, memberRow.canRemove)
        assertEquals(false, memberRow.canDemote)

        val pendingRow = state.allRows().first { it.id == pending.id }
        assertEquals(MemberStatus.Pending, pendingRow.status)
        assertEquals(pending.email, pendingRow.displayName)
        assertEquals(true, pendingRow.canRevokeInvitation)
        assertEquals(true, pendingRow.canResendInvitation)
    }

    @Test
    fun `rows are ordered self first, then admins, then members, then pending, each A-Z`() = runTest {
        val zedAdmin = Member(profileId = "admin-zed", name = "Zed Admin", role = GroupRole.Admin, tagId = null)
        val annAdmin = Member(profileId = "admin-ann", name = "Ann Admin", role = GroupRole.Admin, tagId = null)
        val zedMember = Member(profileId = "member-zed", name = "Zed Member", role = GroupRole.Member, tagId = null)
        val annMember = Member(profileId = "member-ann", name = "Ann Member", role = GroupRole.Member, tagId = null)
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

        val state = assertIs<MembersTabUiState.Success>(viewModel.state.value)
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
            state.allRows().map { it.id }
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

        val state = assertIs<MembersTabUiState.Success>(viewModel.state.value)
        assertEquals(false, state.canAddMembers)
        assertEquals(2, state.allRows().size)
        assertEquals(true, state.allRows().none { it.status == MemberStatus.Pending })
        assertEquals(true, state.allRows().none { it.hasMenu })
    }

    @Test
    fun `a members load failure reports Error`() = runTest {
        val members = FakeMemberRepository(listMembersError = DomainError.Network())
        val viewModel = viewModel(members = members)
        advanceUntilIdle()

        assertIs<MembersTabUiState.Error>(viewModel.state.value)
    }

    @Test
    fun `retrying after a failed load can reach Success`() = runTest {
        val members = FakeMemberRepository(listMembersError = DomainError.Network())
        val viewModel = viewModel(members = members)
        advanceUntilIdle()
        assertIs<MembersTabUiState.Error>(viewModel.state.value)

        members.listMembersError = null
        viewModel.onRetry()
        advanceUntilIdle()

        assertIs<MembersTabUiState.Success>(viewModel.state.value)
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
        val state = assertIs<MembersTabUiState.Success>(viewModel.state.value)
        assertEquals(MemberStatus.Admin, state.allRows().first { it.id == plainMember.profileId }.status)
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
        assertEquals(otherAdmin.profileId, (viewModel.state.value as MembersTabUiState.Success).actionTargetId)
        advanceUntilIdle()

        val state = assertIs<MembersTabUiState.Success>(viewModel.state.value)
        assertNull(state.actionTargetId)
        assertIs<DomainError.LastAdmin>(state.actionError)
        assertEquals(otherAdmin.profileId, state.failedActionId)
        assertEquals(MemberStatus.Admin, state.allRows().first { it.id == otherAdmin.profileId }.status)
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
        val state = assertIs<MembersTabUiState.Success>(viewModel.state.value)
        assertEquals(true, state.allRows().none { it.id == plainMember.profileId })
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
        val state = assertIs<MembersTabUiState.Success>(viewModel.state.value)
        assertEquals(true, state.allRows().none { it.id == pending.id })
    }

    @Test
    fun `resending an invitation calls through and keeps its row`() = runTest {
        val members = FakeMemberRepository(members = listOf(self), pendingInvitations = listOf(pending))
        val viewModel = viewModel(members = members)
        advanceUntilIdle()

        viewModel.onResendInvitation(pending.id)
        advanceUntilIdle()

        assertEquals(listOf(pending.id), members.resentInvitationIds)
        val state = assertIs<MembersTabUiState.Success>(viewModel.state.value)
        assertNull(state.actionError)
        assertEquals(true, state.allRows().any { it.id == pending.id })
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

        val state = assertIs<MembersTabUiState.Success>(viewModel.state.value)
        assertIs<DomainError.InvitationSentTooRecently>(state.actionError)
        assertEquals(pending.id, state.failedActionId)
    }

    @Test
    fun `refresh is a no-op before the first load succeeds`() = runTest {
        val members = FakeMemberRepository(listMembersError = DomainError.Network())
        val viewModel = viewModel(members = members)
        advanceUntilIdle()
        assertIs<MembersTabUiState.Error>(viewModel.state.value)

        viewModel.onRefresh()
        advanceUntilIdle()

        assertIs<MembersTabUiState.Error>(viewModel.state.value)
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

        val state = assertIs<MembersTabUiState.Success>(viewModel.state.value)
        assertEquals(MemberStatus.Admin, state.allRows().first { it.id == plainMember.profileId }.status)
    }

    @Test
    fun `an admin viewer gets tag sections in order with members placed in them`() = runTest {
        val members = FakeMemberRepository(
            members = listOf(self, taggedMember),
            memberTags = listOf(defenders, forwards)
        )
        val viewModel = viewModel(members = members)
        advanceUntilIdle()

        val state = viewModel.success()
        assertEquals(listOf(defenders.id, forwards.id), state.tags.map { it.id })
        assertEquals(listOf(taggedMember.profileId), state.tags[0].rows.map { it.id })
        assertEquals(emptyList(), state.tags[1].rows)
        assertEquals(listOf(FAKE_USER_ID), state.members.map { it.id })
        assertEquals(true, state.canManageTags)
    }

    @Test
    fun `a member viewer gets no tag sections and sees every member in one list`() = runTest {
        val groups = FakeGroupRepository(myRole = GroupRole.Member)
        val members = FakeMemberRepository(
            members = listOf(otherAdmin, taggedMember.copy(profileId = FAKE_USER_ID)),
            memberTags = listOf(defenders, forwards)
        )
        val viewModel = viewModel(groups = groups, members = members)
        advanceUntilIdle()

        val state = viewModel.success()
        assertEquals(emptyList(), state.tags)
        assertEquals(2, state.members.size)
        assertEquals(false, state.canManageTags)
    }

    @Test
    fun `deleting a tag removes its section after the refresh`() = runTest {
        val changes = DataChanges()
        val members = FakeMemberRepository(
            members = listOf(self),
            memberTags = listOf(defenders, forwards),
            dataChanges = changes
        )
        val viewModel = viewModel(members = members, dataChanges = changes)
        advanceUntilIdle()

        viewModel.onDeleteTag(forwards.id)
        assertEquals(forwards.id, viewModel.success().actionTargetId)
        advanceUntilIdle()

        val state = viewModel.success()
        assertEquals(listOf(defenders.id), state.tags.map { it.id })
        assertNull(state.actionTargetId)
        assertNull(state.actionError)
        assertNull(state.failedActionId)
    }

    @Test
    fun `a failed tag delete reports the error on the tag and keeps the section`() = runTest {
        val members = FakeMemberRepository(
            members = listOf(self),
            memberTags = listOf(defenders, forwards),
            deleteTagError = DomainError.Network()
        )
        val viewModel = viewModel(members = members)
        advanceUntilIdle()

        viewModel.onDeleteTag(forwards.id)
        advanceUntilIdle()

        val state = viewModel.success()
        assertEquals(forwards.id, state.failedActionId)
        assertIs<DomainError.Network>(state.actionError)
        assertNull(state.actionTargetId)
        assertEquals(listOf(defenders.id, forwards.id), state.tags.map { it.id })
    }

    @Test
    fun `moving a tag down swaps it with the next one after the refresh`() = runTest {
        val changes = DataChanges()
        val members = FakeMemberRepository(
            members = listOf(self),
            memberTags = listOf(defenders, forwards),
            dataChanges = changes
        )
        val viewModel = viewModel(members = members, dataChanges = changes)
        advanceUntilIdle()

        viewModel.onMoveTagDown(defenders.id)
        advanceUntilIdle()

        val state = viewModel.success()
        assertEquals(listOf(forwards.id, defenders.id), state.tags.map { it.id })
        assertEquals(false, state.tags[0].reorder.canMoveUp)
        assertEquals(true, state.tags[1].reorder.canMoveUp)
        assertNull(state.actionTargetId)
    }

    @Test
    fun `moving a tag up swaps it with the previous one after the refresh`() = runTest {
        val changes = DataChanges()
        val members = FakeMemberRepository(
            members = listOf(self),
            memberTags = listOf(defenders, forwards),
            dataChanges = changes
        )
        val viewModel = viewModel(members = members, dataChanges = changes)
        advanceUntilIdle()

        viewModel.onMoveTagUp(forwards.id)
        advanceUntilIdle()

        assertEquals(listOf(forwards.id, defenders.id), viewModel.success().tags.map { it.id })
    }

    @Test
    fun `moving the first tag up or the last tag down changes nothing and reports no error`() = runTest {
        val members = FakeMemberRepository(members = listOf(self), memberTags = listOf(defenders, forwards))
        val viewModel = viewModel(members = members)
        advanceUntilIdle()

        viewModel.onMoveTagUp(defenders.id)
        advanceUntilIdle()
        viewModel.onMoveTagDown(forwards.id)
        advanceUntilIdle()

        val state = viewModel.success()
        assertEquals(listOf(defenders.id, forwards.id), state.tags.map { it.id })
        assertEquals(listOf(defenders.id, forwards.id), members.tags.map { it.id })
        assertNull(state.actionTargetId)
        assertNull(state.actionError)
        assertNull(state.failedActionId)
    }

    @Test
    fun `a failed tag reorder reports the error on the tag and keeps the order`() = runTest {
        val members = FakeMemberRepository(
            members = listOf(self),
            memberTags = listOf(defenders, forwards),
            setTagOrderError = DomainError.TagListChanged()
        )
        val viewModel = viewModel(members = members)
        advanceUntilIdle()

        viewModel.onMoveTagDown(defenders.id)
        advanceUntilIdle()

        val state = viewModel.success()
        assertEquals(defenders.id, state.failedActionId)
        assertIs<DomainError.TagListChanged>(state.actionError)
        assertNull(state.actionTargetId)
        assertEquals(listOf(defenders.id, forwards.id), state.tags.map { it.id })
    }

    @Test
    fun `setting a member tag moves the row at once and marks it as moving`() = runTest {
        val members = FakeMemberRepository(
            members = listOf(self, taggedMember),
            memberTags = listOf(defenders, forwards)
        )
        val viewModel = viewModel(members = members)
        advanceUntilIdle()

        viewModel.onSetMemberTag(taggedMember.profileId, forwards.id)

        val state = viewModel.success()
        assertEquals(emptyList(), state.tags[0].rows)
        assertEquals(listOf(taggedMember.profileId), state.tags[1].rows.map { it.id })
        assertEquals(forwards.id, state.tags[1].rows.single().tagId)
        assertEquals(taggedMember.profileId, state.movingProfileId)
        assertEquals(taggedMember.profileId, state.actionTargetId)
        assertEquals(defenders.id, members.members.first { it.profileId == taggedMember.profileId }.tagId)
    }

    @Test
    fun `a successful member move keeps the row in the new tag after the refresh`() = runTest {
        val changes = DataChanges()
        val members = FakeMemberRepository(
            members = listOf(self, taggedMember),
            memberTags = listOf(defenders, forwards),
            dataChanges = changes
        )
        val viewModel = viewModel(members = members, dataChanges = changes)
        advanceUntilIdle()

        viewModel.onSetMemberTag(taggedMember.profileId, forwards.id)
        advanceUntilIdle()

        val state = viewModel.success()
        assertEquals(forwards.id, members.members.first { it.profileId == taggedMember.profileId }.tagId)
        assertEquals(emptyList(), state.tags[0].rows)
        assertEquals(listOf(taggedMember.profileId), state.tags[1].rows.map { it.id })
        assertEquals(false, state.members.any { it.id == taggedMember.profileId })
        assertNull(state.movingProfileId)
        assertNull(state.actionTargetId)
        assertNull(state.moveError)
        assertEquals(false, state.isRefreshing)
    }

    @Test
    fun `moving an untagged member into a tag takes the row out of the untagged list`() = runTest {
        val changes = DataChanges()
        val members = FakeMemberRepository(
            members = listOf(self, plainMember),
            memberTags = listOf(defenders),
            dataChanges = changes
        )
        val viewModel = viewModel(members = members, dataChanges = changes)
        advanceUntilIdle()

        viewModel.onSetMemberTag(plainMember.profileId, defenders.id)
        val optimistic = viewModel.success()
        assertEquals(listOf(FAKE_USER_ID), optimistic.members.map { it.id })
        assertEquals(listOf(plainMember.profileId), optimistic.tags.single().rows.map { it.id })
        advanceUntilIdle()

        val state = viewModel.success()
        assertEquals(listOf(FAKE_USER_ID), state.members.map { it.id })
        assertEquals(listOf(plainMember.profileId), state.tags.single().rows.map { it.id })
    }

    @Test
    fun `moving a member to Untagged puts the row back in the untagged list`() = runTest {
        val changes = DataChanges()
        val members = FakeMemberRepository(
            members = listOf(self, taggedMember),
            memberTags = listOf(defenders, forwards),
            dataChanges = changes
        )
        val viewModel = viewModel(members = members, dataChanges = changes)
        advanceUntilIdle()

        viewModel.onSetMemberTag(taggedMember.profileId, null)
        val optimistic = viewModel.success()
        assertEquals(emptyList(), optimistic.tags[0].rows)
        assertEquals(true, optimistic.members.any { it.id == taggedMember.profileId && it.tagId == null })
        assertEquals(taggedMember.profileId, optimistic.movingProfileId)
        advanceUntilIdle()

        val state = viewModel.success()
        assertNull(members.members.first { it.profileId == taggedMember.profileId }.tagId)
        assertEquals(emptyList(), state.tags[0].rows)
        assertEquals(true, state.members.any { it.id == taggedMember.profileId })
        assertNull(state.movingProfileId)
        assertNull(state.actionTargetId)
    }

    @Test
    fun `a failed member move restores the previous rows and reports a move error only`() = runTest {
        val members = FakeMemberRepository(
            members = listOf(self, taggedMember),
            memberTags = listOf(defenders, forwards),
            setMemberTagError = DomainError.Network()
        )
        val viewModel = viewModel(members = members)
        advanceUntilIdle()
        val before = viewModel.success()

        viewModel.onSetMemberTag(taggedMember.profileId, forwards.id)
        advanceUntilIdle()

        val state = viewModel.success()
        assertEquals(before.tags, state.tags)
        assertEquals(before.members, state.members)
        assertIs<DomainError.Network>(state.moveError)
        assertNull(state.actionError)
        assertNull(state.failedActionId)
        assertNull(state.movingProfileId)
        assertNull(state.actionTargetId)
    }

    @Test
    fun `a member move is ignored while another action is in flight`() = runTest {
        val members = FakeMemberRepository(
            members = listOf(self, otherAdmin, taggedMember),
            memberTags = listOf(defenders, forwards)
        )
        val viewModel = viewModel(members = members)
        advanceUntilIdle()

        viewModel.onPromote(otherAdmin.profileId)
        viewModel.onSetMemberTag(taggedMember.profileId, forwards.id)

        val state = viewModel.success()
        assertEquals(otherAdmin.profileId, state.actionTargetId)
        assertNull(state.movingProfileId)
        assertEquals(listOf(taggedMember.profileId), state.tags[0].rows.map { it.id })
        advanceUntilIdle()
        assertEquals(defenders.id, members.members.first { it.profileId == taggedMember.profileId }.tagId)
    }

    @Test
    fun `a member move to the tag the member already has is ignored`() = runTest {
        val members = FakeMemberRepository(
            members = listOf(self, taggedMember),
            memberTags = listOf(defenders, forwards)
        )
        val viewModel = viewModel(members = members)
        advanceUntilIdle()
        val before = viewModel.success()

        viewModel.onSetMemberTag(taggedMember.profileId, defenders.id)
        viewModel.onSetMemberTag(self.profileId, null)

        assertEquals(before, viewModel.success())
        advanceUntilIdle()
        assertEquals(defenders.id, members.members.first { it.profileId == taggedMember.profileId }.tagId)
        assertNull(members.members.first { it.profileId == self.profileId }.tagId)
    }

    @Test
    fun `a member move to a tag that does not exist is ignored`() = runTest {
        val members = FakeMemberRepository(
            members = listOf(self, taggedMember),
            memberTags = listOf(defenders)
        )
        val viewModel = viewModel(members = members)
        advanceUntilIdle()
        val before = viewModel.success()

        viewModel.onSetMemberTag(taggedMember.profileId, "tag-missing")

        assertEquals(before, viewModel.success())
        advanceUntilIdle()
        assertEquals(defenders.id, members.members.first { it.profileId == taggedMember.profileId }.tagId)
    }

    @Test
    fun `a member move for an unknown profile is ignored`() = runTest {
        val members = FakeMemberRepository(
            members = listOf(self, taggedMember),
            memberTags = listOf(defenders, forwards)
        )
        val viewModel = viewModel(members = members)
        advanceUntilIdle()
        val before = viewModel.success()

        viewModel.onSetMemberTag("profile-missing", forwards.id)

        assertEquals(before, viewModel.success())
    }

    @Test
    fun `a member move before the first load finishes is ignored`() = runTest {
        val members = FakeMemberRepository(
            members = listOf(self, taggedMember),
            memberTags = listOf(defenders, forwards)
        )
        val viewModel = viewModel(members = members)

        viewModel.onSetMemberTag(taggedMember.profileId, forwards.id)
        assertEquals(MembersTabUiState.Loading, viewModel.state.value)
        advanceUntilIdle()

        assertEquals(defenders.id, members.members.first { it.profileId == taggedMember.profileId }.tagId)
        assertEquals(listOf(taggedMember.profileId), viewModel.success().tags[0].rows.map { it.id })
    }

    @Test
    fun `a member move clears an earlier move error`() = runTest {
        val members = FakeMemberRepository(
            members = listOf(self, taggedMember),
            memberTags = listOf(defenders, forwards),
            setMemberTagError = DomainError.Network()
        )
        val viewModel = viewModel(members = members)
        advanceUntilIdle()
        viewModel.onSetMemberTag(taggedMember.profileId, forwards.id)
        advanceUntilIdle()
        assertIs<DomainError.Network>(viewModel.success().moveError)

        members.setMemberTagError = null
        viewModel.onSetMemberTag(taggedMember.profileId, forwards.id)

        assertNull(viewModel.success().moveError)
    }

    @Test
    fun `a member move clears an earlier row action error`() = runTest {
        val members = FakeMemberRepository(
            members = listOf(self, otherAdmin, taggedMember),
            memberTags = listOf(defenders, forwards),
            demoteError = DomainError.LastAdmin()
        )
        val viewModel = viewModel(members = members)
        advanceUntilIdle()
        viewModel.onDemote(otherAdmin.profileId)
        advanceUntilIdle()
        assertEquals(otherAdmin.profileId, viewModel.success().failedActionId)

        viewModel.onSetMemberTag(taggedMember.profileId, forwards.id)

        val state = viewModel.success()
        assertNull(state.failedActionId)
        assertNull(state.actionError)
    }

    @Test
    fun `a row action clears an earlier move error`() = runTest {
        val members = FakeMemberRepository(
            members = listOf(self, taggedMember),
            memberTags = listOf(defenders, forwards),
            setMemberTagError = DomainError.Network()
        )
        val viewModel = viewModel(members = members)
        advanceUntilIdle()
        viewModel.onSetMemberTag(taggedMember.profileId, forwards.id)
        advanceUntilIdle()
        assertIs<DomainError.Network>(viewModel.success().moveError)

        viewModel.onMoveTagDown(defenders.id)

        assertNull(viewModel.success().moveError)
    }
}
