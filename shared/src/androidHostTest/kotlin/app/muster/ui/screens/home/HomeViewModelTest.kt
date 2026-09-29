package app.muster.ui.screens.home

import app.muster.data.fake.FakeGroupRepository
import app.muster.data.fake.FakeProfileRepository
import app.muster.domain.error.DomainError
import app.muster.domain.event.DataChange
import app.muster.domain.event.DataChanges
import app.muster.domain.model.Group
import app.muster.domain.model.GroupInvitation
import app.muster.domain.usecase.AcceptGroupInvitationUseCase
import app.muster.domain.usecase.DeclineGroupInvitationUseCase
import app.muster.domain.usecase.GetMyProfileUseCase
import app.muster.domain.usecase.ListMyGroupsUseCase
import app.muster.domain.usecase.ListPendingInvitationsUseCase
import app.muster.testing.MainDispatcherRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val group = Group(id = "g1", name = "Westgate Wednesday 7s")
    private val invitation = GroupInvitation(
        id = "i1",
        group = Group(id = "g2", name = "Thornbury Thursday"),
        invitedByName = "Dan Whelan"
    )

    private fun viewModel(
        profiles: FakeProfileRepository = FakeProfileRepository(),
        groups: FakeGroupRepository = FakeGroupRepository(),
        dataChanges: DataChanges = DataChanges()
    ) = HomeViewModel(
        getMyProfile = GetMyProfileUseCase(profiles),
        listMyGroups = ListMyGroupsUseCase(groups),
        listPendingInvitations = ListPendingInvitationsUseCase(groups),
        acceptGroupInvitation = AcceptGroupInvitationUseCase(groups),
        declineGroupInvitation = DeclineGroupInvitationUseCase(groups),
        dataChanges = dataChanges
    )

    @Test
    fun `starts loading`() {
        assertEquals(HomeUiState.Loading, viewModel().state.value)
    }

    @Test
    fun `a successful load reports groups, invitations and profile`() = runTest {
        val profiles = FakeProfileRepository(canCreateGroups = true)
        val groups = FakeGroupRepository(groups = listOf(group), invitations = listOf(invitation))
        val viewModel = viewModel(profiles, groups)
        advanceUntilIdle()

        val state = assertIs<HomeUiState.Success>(viewModel.state.value)
        assertEquals(listOf(group), state.groups)
        assertEquals(listOf(invitation), state.invitations)
        assertEquals(true, state.canCreateGroups)
        assertEquals(profiles.profile.email, state.signedInEmail)
    }

    @Test
    fun `a profile load failure reports Error`() = runTest {
        val profiles = FakeProfileRepository(getError = DomainError.Network())
        val viewModel = viewModel(profiles = profiles)
        advanceUntilIdle()

        val state = assertIs<HomeUiState.Error>(viewModel.state.value)
        assertIs<DomainError.Network>(state.error)
    }

    @Test
    fun `a groups load failure reports Error`() = runTest {
        val groups = FakeGroupRepository(getMyGroupsError = DomainError.Network())
        val viewModel = viewModel(groups = groups)
        advanceUntilIdle()

        assertIs<HomeUiState.Error>(viewModel.state.value)
    }

    @Test
    fun `accepting an invitation removes it and calls the repository once`() = runTest {
        val groups = FakeGroupRepository(invitations = listOf(invitation))
        val viewModel = viewModel(groups = groups)
        advanceUntilIdle()

        viewModel.onAccept(invitation.id)
        advanceUntilIdle()

        val state = assertIs<HomeUiState.Success>(viewModel.state.value)
        assertEquals(emptyList(), state.invitations)
        assertNull(state.respondingTo)
        assertEquals(listOf(invitation.id), groups.acceptedInvitationIds)
    }

    @Test
    fun `declining an invitation removes it and calls the repository once`() = runTest {
        val groups = FakeGroupRepository(invitations = listOf(invitation))
        val viewModel = viewModel(groups = groups)
        advanceUntilIdle()

        viewModel.onDecline(invitation.id)
        advanceUntilIdle()

        val state = assertIs<HomeUiState.Success>(viewModel.state.value)
        assertEquals(emptyList(), state.invitations)
        assertEquals(listOf(invitation.id), groups.declinedInvitationIds)
    }

    @Test
    fun `a second tap while responding is ignored`() = runTest {
        val groups = FakeGroupRepository(invitations = listOf(invitation))
        val viewModel = viewModel(groups = groups)
        advanceUntilIdle()

        viewModel.onAccept(invitation.id)
        viewModel.onAccept(invitation.id)
        advanceUntilIdle()

        assertEquals(listOf(invitation.id), groups.acceptedInvitationIds)
    }

    @Test
    fun `a failed accept keeps the invitation and clears the in-flight flag`() = runTest {
        val groups = FakeGroupRepository(
            invitations = listOf(invitation),
            acceptError = DomainError.Network()
        )
        val viewModel = viewModel(groups = groups)
        advanceUntilIdle()

        viewModel.onAccept(invitation.id)
        assertEquals(invitation.id, (viewModel.state.value as HomeUiState.Success).respondingTo)
        advanceUntilIdle()

        val state = assertIs<HomeUiState.Success>(viewModel.state.value)
        assertEquals(listOf(invitation), state.invitations)
        assertNull(state.respondingTo)
    }

    @Test
    fun `retrying after a failed load can reach Success`() = runTest {
        val profiles = FakeProfileRepository(getError = DomainError.Network())
        val groups = FakeGroupRepository(groups = listOf(group))
        val viewModel = viewModel(profiles, groups)
        advanceUntilIdle()
        assertIs<HomeUiState.Error>(viewModel.state.value)

        profiles.getError = null
        viewModel.onRetry()
        advanceUntilIdle()

        val state = assertIs<HomeUiState.Success>(viewModel.state.value)
        assertEquals(listOf(group), state.groups)
    }

    @Test
    fun `refresh is a no-op before the first load succeeds`() = runTest {
        val profiles = FakeProfileRepository(getError = DomainError.Network())
        val viewModel = viewModel(profiles = profiles)
        advanceUntilIdle()
        assertIs<HomeUiState.Error>(viewModel.state.value)

        viewModel.onRefresh()
        advanceUntilIdle()

        assertIs<HomeUiState.Error>(viewModel.state.value)
    }

    @Test
    fun `a group created elsewhere reaches the list`() = runTest {
        val changes = DataChanges()
        val groups = FakeGroupRepository(dataChanges = changes)
        val viewModel = viewModel(groups = groups, dataChanges = changes)
        advanceUntilIdle()
        assertEquals(emptyList(), (viewModel.state.value as HomeUiState.Success).groups)

        groups.createGroup("Westgate Wednesday 7s")
        advanceUntilIdle()

        val state = assertIs<HomeUiState.Success>(viewModel.state.value)
        assertEquals(listOf("Westgate Wednesday 7s"), state.groups.map { it.name })
    }

    @Test
    fun `a MyGroups change shows the refresh indicator`() = runTest {
        val changes = DataChanges()
        val viewModel = viewModel(dataChanges = changes)
        advanceUntilIdle()

        changes.notify(DataChange.MyGroups)
        runCurrent()

        assertEquals(true, (viewModel.state.value as HomeUiState.Success).isRefreshing)
    }

    @Test
    fun `a MyGroups change before the first load succeeds is ignored`() = runTest {
        val changes = DataChanges()
        val profiles = FakeProfileRepository(getError = DomainError.Network())
        val viewModel = viewModel(profiles = profiles, dataChanges = changes)
        advanceUntilIdle()
        assertIs<HomeUiState.Error>(viewModel.state.value)

        changes.notify(DataChange.MyGroups)
        advanceUntilIdle()

        assertIs<HomeUiState.Error>(viewModel.state.value)
    }
}
