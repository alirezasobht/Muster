package app.muster.ui.screens.addplayers

import app.muster.data.fake.FakeEventRepository
import app.muster.domain.error.DomainError
import app.muster.domain.model.GroupRole
import app.muster.domain.model.InviteeCandidates
import app.muster.domain.model.Member
import app.muster.domain.usecase.AddPlayersUseCase
import app.muster.domain.usecase.GetInviteeCandidatesUseCase
import app.muster.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class AddPlayersViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val eventId = "event-1"
    private val groupId = "group-1"

    // capacity 3, one already in -> 2 free slots, matching the old fixture default.
    private val defaultCandidates = listOf(
        Member(profileId = "player-2", name = "Dan Whelan", role = GroupRole.Member),
        Member(profileId = "player-3", name = "Sam Okafor", role = GroupRole.Member),
        Member(profileId = "player-4", name = "Priya Nair", role = GroupRole.Member),
        Member(profileId = "player-5", name = "Leo Fanning", role = GroupRole.Member)
    )

    private fun result(
        members: List<Member> = defaultCandidates,
        capacity: Int = 3,
        freeSlots: Int = 2,
        queueLength: Int = 0
    ) = InviteeCandidates(members = members, capacity = capacity, freeSlots = freeSlots, queueLength = queueLength)

    // Candidate filtering and capacity math belong to GetInviteeCandidatesUseCase
    // and are tested there (GetInviteeCandidatesUseCaseTest). Mocking it here
    // keeps these tests to what the ViewModel itself does: mapping its result
    // to display state, selection order, and the confirm/write flow.
    private fun mockCandidates(
        result: InviteeCandidates = result(),
        error: DomainError? = null
    ): GetInviteeCandidatesUseCase {
        val mock = mockk<GetInviteeCandidatesUseCase>()
        coEvery { mock(eventId, groupId) } answers { error?.let { throw it } ?: result }
        return mock
    }

    private fun viewModel(
        events: FakeEventRepository = FakeEventRepository(),
        candidates: GetInviteeCandidatesUseCase = mockCandidates()
    ) = AddPlayersViewModel(
        eventId = eventId,
        groupId = groupId,
        getInviteeCandidates = candidates,
        addPlayers = AddPlayersUseCase(events)
    )

    @Test
    fun `starts loading`() {
        assertEquals(AddPlayersUiState.Loading, viewModel().state.value)
    }

    @Test
    fun `maps the use case result into display state, sorted by name`() = runTest {
        val viewModel = viewModel(candidates = mockCandidates(result = result(capacity = 5, freeSlots = 3, queueLength = 2)))
        advanceUntilIdle()

        val state = assertIs<AddPlayersUiState.Success>(viewModel.state.value)
        // Given in id order player-2..5 (Dan, Sam, Priya, Leo); displayed
        // alphabetically instead — that sort is the ViewModel's job, not
        // the use case's.
        assertEquals(listOf("Dan Whelan", "Leo Fanning", "Priya Nair", "Sam Okafor"), state.candidates.map { it.name })
        assertEquals(5, state.capacity)
        assertEquals(3, state.freeSlots)
        assertEquals(2, state.queueLength)
    }

    @Test
    fun `an empty candidate list is reported as empty`() = runTest {
        val viewModel = viewModel(candidates = mockCandidates(result = result(members = emptyList())))
        advanceUntilIdle()

        assertTrue(assertIs<AddPlayersUiState.Success>(viewModel.state.value).isEmpty)
    }

    @Test
    fun `onToggle adds and removes a candidate from the selection`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onToggle("player-3")
        assertEquals(listOf("player-3"), assertIs<AddPlayersUiState.Success>(viewModel.state.value).selectedIds)

        viewModel.onToggle("player-3")
        assertEquals(emptyList(), assertIs<AddPlayersUiState.Success>(viewModel.state.value).selectedIds)
    }

    @Test
    fun `checking in reverse of display order keeps the check order, not the display order`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        // Candidates are name-sorted: Leo Fanning, Priya Nair, Sam Okafor,
        // Dan Whelan — checked in a different order here.
        viewModel.onToggle("player-3")
        viewModel.onToggle("player-5")

        val state = assertIs<AddPlayersUiState.Success>(viewModel.state.value)
        assertEquals(listOf("player-3", "player-5"), state.selectedIds)
    }

    @Test
    fun `unchecking a pick removes it and shifts the rest up`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onToggle("player-2")
        viewModel.onToggle("player-3")
        viewModel.onToggle("player-4")
        viewModel.onToggle("player-2")

        val state = assertIs<AddPlayersUiState.Success>(viewModel.state.value)
        assertEquals(listOf("player-3", "player-4"), state.selectedIds)
    }

    @Test
    fun `re-checking a pick sends it to the back, not its old spot`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onToggle("player-2")
        viewModel.onToggle("player-3")
        viewModel.onToggle("player-2")
        viewModel.onToggle("player-2")

        val state = assertIs<AddPlayersUiState.Success>(viewModel.state.value)
        assertEquals(listOf("player-3", "player-2"), state.selectedIds)
    }

    @Test
    fun `onConfirm does nothing with an empty selection`() = runTest {
        val events = FakeEventRepository()
        val viewModel = viewModel(events = events)
        advanceUntilIdle()

        viewModel.onConfirm()
        advanceUntilIdle()

        assertEquals(emptyList(), events.addPlayersCalls)
    }

    @Test
    fun `onConfirm sends ids in check order, not display order`() = runTest {
        val events = FakeEventRepository()
        val viewModel = viewModel(events = events)
        advanceUntilIdle()

        // Candidates are name-sorted: Leo Fanning, Priya Nair, Sam Okafor,
        // Dan Whelan — checked in a different order here.
        viewModel.onToggle("player-3")
        viewModel.onToggle("player-5")
        viewModel.onConfirm()
        advanceUntilIdle()

        assertEquals(listOf(listOf("player-3", "player-5")), events.addPlayersCalls)
    }

    @Test
    fun `onConfirm emits exit exactly once on success and clears adding`() = runTest {
        val events = FakeEventRepository()
        val viewModel = viewModel(events = events)
        advanceUntilIdle()

        var emittedCount = 0
        val collector = launch { viewModel.exit.collect { emittedCount++ } }

        viewModel.onToggle("player-3")
        viewModel.onConfirm()
        advanceUntilIdle()

        assertEquals(1, emittedCount)
        val state = assertIs<AddPlayersUiState.Success>(viewModel.state.value)
        assertEquals(false, state.adding)
        collector.cancel()
    }

    @Test
    fun `a failed onConfirm surfaces the error and does not emit exit`() = runTest {
        val events = FakeEventRepository(addPlayersError = DomainError.EventFrozen())
        val viewModel = viewModel(events = events)
        advanceUntilIdle()

        var emitted = false
        val collector = launch { viewModel.exit.collect { emitted = true } }

        viewModel.onToggle("player-3")
        viewModel.onConfirm()
        advanceUntilIdle()

        assertFalse(emitted)
        val state = assertIs<AddPlayersUiState.Success>(viewModel.state.value)
        assertIs<DomainError.EventFrozen>(state.error)
        assertEquals(false, state.adding)
        collector.cancel()
    }

    @Test
    fun `a second onConfirm while adding is ignored`() = runTest {
        val events = FakeEventRepository()
        val viewModel = viewModel(events = events)
        advanceUntilIdle()

        viewModel.onToggle("player-3")
        viewModel.onConfirm()
        viewModel.onConfirm()
        advanceUntilIdle()

        assertEquals(1, events.addPlayersCalls.size)
    }

    @Test
    fun `onToggle while adding is ignored`() = runTest {
        val events = FakeEventRepository()
        val viewModel = viewModel(events = events)
        advanceUntilIdle()

        viewModel.onToggle("player-3")
        viewModel.onConfirm()
        viewModel.onToggle("player-4")
        advanceUntilIdle()

        // The extra toggle landed before the write completed and was dropped,
        // so only the original pick was sent.
        assertEquals(listOf(listOf("player-3")), events.addPlayersCalls)
    }

    @Test
    fun `a load failure reports Error`() = runTest {
        val viewModel = viewModel(candidates = mockCandidates(error = DomainError.Network()))
        advanceUntilIdle()

        assertIs<DomainError.Network>(assertIs<AddPlayersUiState.Error>(viewModel.state.value).error)
    }

    @Test
    fun `retrying after a failed load can reach Success`() = runTest {
        val candidates = mockCandidates(error = DomainError.Network())
        val viewModel = viewModel(candidates = candidates)
        advanceUntilIdle()
        assertIs<AddPlayersUiState.Error>(viewModel.state.value)

        coEvery { candidates(eventId, groupId) } returns result()
        viewModel.onRetry()
        advanceUntilIdle()

        assertIs<AddPlayersUiState.Success>(viewModel.state.value)
    }
}
