package app.muster.ui.screens.newgroup

import app.muster.data.fake.FakeGroupRepository
import app.muster.domain.error.DomainError
import app.muster.domain.usecase.CreateGroupUseCase
import app.muster.testing.MainDispatcherRule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NewGroupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(groups: FakeGroupRepository = FakeGroupRepository()) =
        NewGroupViewModel(CreateGroupUseCase(groups))

    @Test
    fun `starts empty`() {
        assertEquals(NewGroupUiState(), viewModel().state.value)
    }

    @Test
    fun `creating reports the group`() = runTest {
        val viewModel = viewModel()
        viewModel.onNameChange("Westgate Wednesday 7s")
        viewModel.onCreate()
        assertTrue(viewModel.state.value.creating)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(false, state.creating)
        assertEquals("Westgate Wednesday 7s", state.created?.name)
        assertNull(state.error)
    }

    @Test
    fun `the name is trimmed before creating`() = runTest {
        val groups = FakeGroupRepository()
        val viewModel = viewModel(groups)
        viewModel.onNameChange("  Westgate Wednesday 7s  ")
        viewModel.onCreate()
        advanceUntilIdle()

        assertEquals(listOf("Westgate Wednesday 7s"), groups.createdGroupNames)
    }

    @Test
    fun `a blank name creates nothing`() = runTest {
        val groups = FakeGroupRepository()
        val viewModel = viewModel(groups)
        viewModel.onNameChange("   ")
        viewModel.onCreate()
        advanceUntilIdle()

        assertTrue(groups.createdGroupNames.isEmpty())
        assertNull(viewModel.state.value.created)
    }

    @Test
    fun `a second tap while creating is ignored`() = runTest {
        val groups = FakeGroupRepository()
        val viewModel = viewModel(groups)
        viewModel.onNameChange("Westgate Wednesday 7s")
        viewModel.onCreate()
        viewModel.onCreate()
        advanceUntilIdle()

        assertEquals(1, groups.createdGroupNames.size)
    }

    @Test
    fun `a failed create surfaces the error`() = runTest {
        val groups = FakeGroupRepository(createGroupError = DomainError.NotAllowedToCreateGroups())
        val viewModel = viewModel(groups)
        viewModel.onNameChange("Westgate Wednesday 7s")
        viewModel.onCreate()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertIs<DomainError.NotAllowedToCreateGroups>(state.error)
        assertEquals(false, state.creating)
        assertNull(state.created)
    }

    @Test
    fun `typing clears the error`() = runTest {
        val groups = FakeGroupRepository(createGroupError = DomainError.Network())
        val viewModel = viewModel(groups)
        viewModel.onNameChange("Westgate")
        viewModel.onCreate()
        advanceUntilIdle()
        assertIs<DomainError.Network>(viewModel.state.value.error)

        viewModel.onNameChange("Westgate Wednesday 7s")
        assertNull(viewModel.state.value.error)
    }

    // Navigating away and back must not recreate the group.
    @Test
    fun `handling the result resets the screen`() = runTest {
        val viewModel = viewModel()
        viewModel.onNameChange("Westgate Wednesday 7s")
        viewModel.onCreate()
        advanceUntilIdle()

        viewModel.onCreatedHandled()
        assertEquals(NewGroupUiState(), viewModel.state.value)
    }
}
