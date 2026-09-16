package app.muster.ui.screens.group

import app.muster.data.fake.FakeGroupRepository
import app.muster.data.fake.FakeMemberRepository
import app.muster.domain.error.DomainError
import app.muster.domain.model.GroupRole
import app.muster.domain.usecase.ArchiveGroupUseCase
import app.muster.domain.usecase.GetMyGroupRoleUseCase
import app.muster.domain.usecase.LeaveGroupUseCase
import app.muster.testing.MainDispatcherRule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GroupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(
        groupId: String = "group-1",
        groupName: String = "Westgate Wednesday 7s",
        groups: FakeGroupRepository = FakeGroupRepository(),
        members: FakeMemberRepository = FakeMemberRepository()
    ) = GroupViewModel(
        groupId = groupId,
        groupName = groupName,
        getMyGroupRole = GetMyGroupRoleUseCase(groups),
        leaveGroup = LeaveGroupUseCase(members),
        archiveGroup = ArchiveGroupUseCase(groups)
    )

    @Test
    fun `starts loading with the name already known`() {
        val state = viewModel(groupName = "Westgate Wednesday 7s").state.value
        assertIs<GroupUiState.Loading>(state)
        assertEquals("Westgate Wednesday 7s", state.groupName)
    }

    @Test
    fun `a successful load reports the role, keeping the name`() = runTest {
        val viewModel = viewModel(
            groupName = "Westgate Wednesday 7s",
            groups = FakeGroupRepository(myRole = GroupRole.Admin)
        )
        advanceUntilIdle()

        val state = assertIs<GroupUiState.Success>(viewModel.state.value)
        assertEquals("Westgate Wednesday 7s", state.groupName)
        assertEquals(GroupRole.Admin, state.myRole)
    }

    @Test
    fun `an admin role reports as admin`() = runTest {
        val viewModel = viewModel(groups = FakeGroupRepository(myRole = GroupRole.Admin))
        advanceUntilIdle()

        assertEquals(true, (viewModel.state.value as GroupUiState.Success).isAdmin)
    }

    @Test
    fun `a member role does not report as admin`() = runTest {
        val viewModel = viewModel(groups = FakeGroupRepository(myRole = GroupRole.Member))
        advanceUntilIdle()

        assertEquals(false, (viewModel.state.value as GroupUiState.Success).isAdmin)
    }

    @Test
    fun `an admin's overflow menu offers Archive above Leave`() = runTest {
        val viewModel = viewModel(groups = FakeGroupRepository(myRole = GroupRole.Admin))
        advanceUntilIdle()

        assertEquals(
            listOf(GroupOverflowAction.Archive, GroupOverflowAction.Leave),
            (viewModel.state.value as GroupUiState.Success).overflowActions
        )
    }

    @Test
    fun `a member's overflow menu offers only Leave`() = runTest {
        val viewModel = viewModel(groups = FakeGroupRepository(myRole = GroupRole.Member))
        advanceUntilIdle()

        assertEquals(
            listOf(GroupOverflowAction.Leave),
            (viewModel.state.value as GroupUiState.Success).overflowActions
        )
    }

    @Test
    fun `a successful load starts on the Events tab`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(GroupTab.Events, (viewModel.state.value as GroupUiState.Success).selectedTab)
    }

    @Test
    fun `a failed role fetch reports Error but keeps the name`() = runTest {
        val groups = FakeGroupRepository(getMyRoleError = DomainError.Network())
        val viewModel = viewModel(groupName = "Westgate Wednesday 7s", groups = groups)
        advanceUntilIdle()

        val state = assertIs<GroupUiState.Error>(viewModel.state.value)
        assertEquals("Westgate Wednesday 7s", state.groupName)
        assertIs<DomainError.Network>(state.error)
    }

    @Test
    fun `retrying after a failed load can reach Success`() = runTest {
        val groups = FakeGroupRepository(getMyRoleError = DomainError.Network())
        val viewModel = viewModel(groups = groups)
        advanceUntilIdle()
        assertIs<GroupUiState.Error>(viewModel.state.value)

        groups.getMyRoleError = null
        viewModel.onRetry()
        advanceUntilIdle()

        assertIs<GroupUiState.Success>(viewModel.state.value)
    }

    @Test
    fun `selecting a tab switches the state`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onTabSelected(GroupTab.Members)

        assertEquals(GroupTab.Members, (viewModel.state.value as GroupUiState.Success).selectedTab)
    }

    @Test
    fun `selecting a tab before the load completes does nothing`() = runTest {
        val viewModel = viewModel()

        viewModel.onTabSelected(GroupTab.Members)

        assertIs<GroupUiState.Loading>(viewModel.state.value)
    }

    @Test
    fun `requesting leave opens a fresh Leave dialog`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onOverflowActionRequested(GroupOverflowAction.Leave)

        val dialog = (viewModel.state.value as GroupUiState.Success).overflowDialog
        assertEquals(GroupOverflowAction.Leave, dialog?.action)
        assertEquals(false, dialog?.inFlight)
        assertEquals(null, dialog?.error)
    }

    @Test
    fun `requesting archive opens a fresh Archive dialog`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onOverflowActionRequested(GroupOverflowAction.Archive)

        assertEquals(
            GroupOverflowAction.Archive,
            (viewModel.state.value as GroupUiState.Success).overflowDialog?.action
        )
    }

    @Test
    fun `confirming leave calls the repository and marks the group exited`() = runTest {
        val members = FakeMemberRepository()
        val viewModel = viewModel(groupId = "group-1", members = members)
        advanceUntilIdle()

        viewModel.onOverflowActionRequested(GroupOverflowAction.Leave)
        viewModel.onOverflowConfirmed()
        advanceUntilIdle()

        assertEquals(listOf("group-1"), members.leftGroupIds)
        val state = assertIs<GroupUiState.Success>(viewModel.state.value)
        assertEquals(true, state.exitedGroup)
        assertEquals(null, state.overflowDialog)
    }

    @Test
    fun `confirming archive calls the repository and marks the group exited`() = runTest {
        val groups = FakeGroupRepository()
        val viewModel = viewModel(groupId = "group-1", groups = groups)
        advanceUntilIdle()

        viewModel.onOverflowActionRequested(GroupOverflowAction.Archive)
        viewModel.onOverflowConfirmed()
        advanceUntilIdle()

        assertEquals(listOf("group-1"), groups.archivedGroupIds)
        val state = assertIs<GroupUiState.Success>(viewModel.state.value)
        assertEquals(true, state.exitedGroup)
    }

    @Test
    fun `confirming without a dialog open does nothing`() = runTest {
        val members = FakeMemberRepository()
        val viewModel = viewModel(members = members)
        advanceUntilIdle()

        viewModel.onOverflowConfirmed()
        advanceUntilIdle()

        assertEquals(emptyList(), members.leftGroupIds)
    }

    @Test
    fun `a second confirm while one is in flight is ignored`() = runTest {
        val members = FakeMemberRepository()
        val viewModel = viewModel(members = members)
        advanceUntilIdle()

        viewModel.onOverflowActionRequested(GroupOverflowAction.Leave)
        viewModel.onOverflowConfirmed()
        viewModel.onOverflowConfirmed()
        advanceUntilIdle()

        assertEquals(1, members.leftGroupIds.size)
    }

    // The real path: group_keeps_an_admin rejects the last admin leaving.
    @Test
    fun `a failed leave clears in-flight, reports the error and does not exit`() = runTest {
        val members = FakeMemberRepository(leaveError = DomainError.LastAdmin())
        val viewModel = viewModel(members = members)
        advanceUntilIdle()

        viewModel.onOverflowActionRequested(GroupOverflowAction.Leave)
        viewModel.onOverflowConfirmed()
        advanceUntilIdle()

        val state = assertIs<GroupUiState.Success>(viewModel.state.value)
        assertEquals(false, state.exitedGroup)
        val dialog = state.overflowDialog
        assertEquals(GroupOverflowAction.Leave, dialog?.action)
        assertEquals(false, dialog?.inFlight)
        assertIs<DomainError.LastAdmin>(dialog?.error)
    }

    @Test
    fun `a failed archive keeps the group and reports the error`() = runTest {
        val groups = FakeGroupRepository(archiveError = DomainError.Network())
        val viewModel = viewModel(groups = groups)
        advanceUntilIdle()

        viewModel.onOverflowActionRequested(GroupOverflowAction.Archive)
        viewModel.onOverflowConfirmed()
        advanceUntilIdle()

        val state = assertIs<GroupUiState.Success>(viewModel.state.value)
        assertEquals(false, state.exitedGroup)
        assertIs<DomainError.Network>(state.overflowDialog?.error)
    }

    // Retrying is tapping the same button again: same dialog, same action,
    // error cleared before the call goes out.
    @Test
    fun `retrying a failed leave can succeed`() = runTest {
        val members = FakeMemberRepository(leaveError = DomainError.LastAdmin())
        val viewModel = viewModel(members = members)
        advanceUntilIdle()
        viewModel.onOverflowActionRequested(GroupOverflowAction.Leave)
        viewModel.onOverflowConfirmed()
        advanceUntilIdle()
        assertIs<DomainError.LastAdmin>((viewModel.state.value as GroupUiState.Success).overflowDialog?.error)

        members.leaveError = null
        viewModel.onOverflowConfirmed()
        advanceUntilIdle()

        val state = assertIs<GroupUiState.Success>(viewModel.state.value)
        assertEquals(true, state.exitedGroup)
        assertEquals(null, state.overflowDialog)
    }

    @Test
    fun `dismissing closes the dialog entirely`() = runTest {
        val members = FakeMemberRepository(leaveError = DomainError.LastAdmin())
        val viewModel = viewModel(members = members)
        advanceUntilIdle()
        viewModel.onOverflowActionRequested(GroupOverflowAction.Leave)
        viewModel.onOverflowConfirmed()
        advanceUntilIdle()
        assertIs<DomainError.LastAdmin>((viewModel.state.value as GroupUiState.Success).overflowDialog?.error)

        viewModel.onOverflowDialogDismissed()

        assertEquals(null, (viewModel.state.value as GroupUiState.Success).overflowDialog)
    }

    // Regression: a stale error used to survive a cancelled dialog and leak
    // into whichever dialog opened next. Opening always replaces the whole
    // OverflowDialogState now, so there is nothing left to leak.
    @Test
    fun `a failed leave does not leak its error into a later archive dialog`() = runTest {
        val members = FakeMemberRepository(leaveError = DomainError.LastAdmin())
        val viewModel = viewModel(members = members)
        advanceUntilIdle()
        viewModel.onOverflowActionRequested(GroupOverflowAction.Leave)
        viewModel.onOverflowConfirmed()
        advanceUntilIdle()
        viewModel.onOverflowDialogDismissed()

        viewModel.onOverflowActionRequested(GroupOverflowAction.Archive)

        val dialog = (viewModel.state.value as GroupUiState.Success).overflowDialog
        assertEquals(GroupOverflowAction.Archive, dialog?.action)
        assertEquals(null, dialog?.error)
    }

    @Test
    fun `onExitedHandled clears the exited flag`() = runTest {
        val viewModel = viewModel(members = FakeMemberRepository())
        advanceUntilIdle()

        viewModel.onOverflowActionRequested(GroupOverflowAction.Leave)
        viewModel.onOverflowConfirmed()
        advanceUntilIdle()
        assertEquals(true, (viewModel.state.value as GroupUiState.Success).exitedGroup)

        viewModel.onExitedHandled()

        assertEquals(false, (viewModel.state.value as GroupUiState.Success).exitedGroup)
    }
}
