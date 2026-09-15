package app.muster.ui.screens.group

import app.muster.data.fake.FakeGroupRepository
import app.muster.domain.error.DomainError
import app.muster.domain.model.GroupRole
import app.muster.domain.usecase.GetMyGroupRoleUseCase
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
        groups: FakeGroupRepository = FakeGroupRepository()
    ) = GroupViewModel(
        groupId = groupId,
        groupName = groupName,
        getMyGroupRole = GetMyGroupRoleUseCase(groups)
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
}
