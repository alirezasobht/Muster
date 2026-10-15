package app.muster.ui.screens.editgroup

import app.muster.data.fake.FakeGroupRepository
import app.muster.domain.error.DomainError
import app.muster.domain.usecase.UpdateGroupUseCase
import app.muster.testing.MainDispatcherRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class EditGroupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(groups: FakeGroupRepository = FakeGroupRepository()) = EditGroupViewModel(
        groupId = "group-1",
        groupName = "Westgate Wednesday 7s",
        updateGroup = UpdateGroupUseCase(groups)
    )

    @Test
    fun `starts with the current name and nothing to save`() {
        val state = viewModel().state.value
        assertEquals("Westgate Wednesday 7s", state.name)
        assertFalse(state.canSave)
    }

    @Test
    fun `a changed name can be saved`() {
        val viewModel = viewModel()
        viewModel.onNameChange("Westgate Thursday 7s")
        assertTrue(viewModel.state.value.canSave)
    }

    @Test
    fun `whitespace alone is not a change`() {
        val viewModel = viewModel()
        viewModel.onNameChange("  Westgate Wednesday 7s  ")
        assertFalse(viewModel.state.value.canSave)
    }

    @Test
    fun `a blank name cannot be saved`() {
        val viewModel = viewModel()
        viewModel.onNameChange("   ")
        assertFalse(viewModel.state.value.canSave)
    }

    @Test
    fun `saving trims and reports saved`() = runTest {
        val groups = FakeGroupRepository()
        val viewModel = viewModel(groups)
        viewModel.onNameChange("  Westgate Thursday 7s  ")
        viewModel.onSave()
        assertTrue(viewModel.state.value.saving)
        advanceUntilIdle()

        assertEquals(listOf("Westgate Thursday 7s"), groups.updatedGroupNames)
        assertTrue(viewModel.state.value.saved)
        assertFalse(viewModel.state.value.saving)
    }

    @Test
    fun `an unchanged name saves nothing`() = runTest {
        val groups = FakeGroupRepository()
        val viewModel = viewModel(groups)
        viewModel.onSave()
        advanceUntilIdle()

        assertTrue(groups.updatedGroupNames.isEmpty())
    }

    @Test
    fun `a second tap while saving is ignored`() = runTest {
        val groups = FakeGroupRepository()
        val viewModel = viewModel(groups)
        viewModel.onNameChange("Westgate Thursday 7s")
        viewModel.onSave()
        viewModel.onSave()
        advanceUntilIdle()

        assertEquals(1, groups.updatedGroupNames.size)
    }

    @Test
    fun `a failed save surfaces a form error`() = runTest {
        val viewModel = viewModel(FakeGroupRepository(updateGroupError = DomainError.Network()))
        viewModel.onNameChange("Westgate Thursday 7s")
        viewModel.onSave()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertIs<DomainError.Network>(state.error)
        assertNull(state.nameError)
        assertFalse(state.saved)
        assertTrue(state.canSave)
    }

    @Test
    fun `a server-rejected name is a field error`() = runTest {
        val viewModel = viewModel(FakeGroupRepository(updateGroupError = DomainError.InvalidName()))
        viewModel.onNameChange("W")
        viewModel.onSave()
        advanceUntilIdle()

        assertIs<DomainError.InvalidName>(viewModel.state.value.nameError)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `handling the result clears saved`() = runTest {
        val viewModel = viewModel()
        viewModel.onNameChange("Westgate Thursday 7s")
        viewModel.onSave()
        advanceUntilIdle()

        viewModel.onSavedHandled()
        assertFalse(viewModel.state.value.saved)
    }
}
