package app.muster.ui.screens.setname

import app.muster.data.fake.FakeProfileRepository
import app.muster.domain.error.DomainError
import app.muster.domain.usecase.UpdateNameUseCase
import app.muster.testing.MainDispatcherRule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SetNameViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(profiles: FakeProfileRepository = FakeProfileRepository()) =
        SetNameViewModel(UpdateNameUseCase(profiles))

    @Test
    fun `starts empty`() {
        assertEquals(SetNameUiState(), viewModel().state.value)
    }

    @Test
    fun `saving reports the profile`() = runTest {
        val viewModel = viewModel()
        viewModel.onNameChange("Alex Doyle")
        viewModel.onContinue()
        assertTrue(viewModel.state.value.saving)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(false, state.saving)
        assertEquals("Alex Doyle", state.saved?.name)
        assertNull(state.error)
    }

    @Test
    fun `the name is trimmed before saving`() = runTest {
        val profiles = FakeProfileRepository()
        val viewModel = viewModel(profiles)
        viewModel.onNameChange("  Alex Doyle  ")
        viewModel.onContinue()
        advanceUntilIdle()

        assertEquals("Alex Doyle", profiles.profile.name)
    }

    @Test
    fun `a blank name saves nothing`() = runTest {
        val profiles = FakeProfileRepository()
        val viewModel = viewModel(profiles)
        viewModel.onNameChange("   ")
        viewModel.onContinue()
        advanceUntilIdle()

        assertNull(profiles.profile.name)
        assertNull(viewModel.state.value.saved)
    }

    @Test
    fun `a second tap while saving is ignored`() = runTest {
        val profiles = FakeProfileRepository()
        val viewModel = viewModel(profiles)
        viewModel.onNameChange("Alex")
        viewModel.onContinue()
        viewModel.onContinue()
        advanceUntilIdle()

        assertEquals("Alex", profiles.profile.name)
    }

    @Test
    fun `a failed save surfaces the error`() = runTest {
        val profiles = FakeProfileRepository(updateError = DomainError.Network())
        val viewModel = viewModel(profiles)
        viewModel.onNameChange("Alex Doyle")
        viewModel.onContinue()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertIs<DomainError.Network>(state.error)
        assertEquals(false, state.saving)
        assertNull(state.saved)
    }

    @Test
    fun `typing clears the error`() = runTest {
        val profiles = FakeProfileRepository(updateError = DomainError.Network())
        val viewModel = viewModel(profiles)
        viewModel.onNameChange("Alex")
        viewModel.onContinue()
        advanceUntilIdle()
        assertIs<DomainError.Network>(viewModel.state.value.error)

        viewModel.onNameChange("Alexa")
        assertNull(viewModel.state.value.error)
    }

    // The next account signing in on this device must not inherit the name.
    @Test
    fun `handling the result resets the screen`() = runTest {
        val viewModel = viewModel()
        viewModel.onNameChange("Alex Doyle")
        viewModel.onContinue()
        advanceUntilIdle()

        viewModel.onSavedHandled()
        assertEquals(SetNameUiState(), viewModel.state.value)
    }
}
