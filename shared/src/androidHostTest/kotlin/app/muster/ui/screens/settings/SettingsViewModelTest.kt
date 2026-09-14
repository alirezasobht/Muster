package app.muster.ui.screens.settings

import app.muster.data.fake.FAKE_EMAIL
import app.muster.data.fake.FakeProfileRepository
import app.muster.domain.error.DomainError
import app.muster.domain.usecase.GetMyProfileUseCase
import app.muster.domain.usecase.UpdateNameUseCase
import app.muster.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(profiles: FakeProfileRepository = FakeProfileRepository(name = "Alex Doyle")) =
        SettingsViewModel(
            getMyProfile = GetMyProfileUseCase(profiles),
            updateName = UpdateNameUseCase(profiles)
        )

    @Test
    fun `starts loading`() {
        assertEquals(SettingsUiState.Loading, viewModel().state.value)
    }

    @Test
    fun `a successful load reports the name and email`() = runTest {
        val viewModel = viewModel(FakeProfileRepository(name = "Alex Doyle"))
        advanceUntilIdle()

        val state = assertIs<SettingsUiState.Success>(viewModel.state.value)
        assertEquals("Alex Doyle", state.name)
        assertEquals(FAKE_EMAIL, state.email)
    }

    @Test
    fun `a failed load reports Error`() = runTest {
        val profiles = FakeProfileRepository(name = "Alex Doyle", getError = DomainError.Network())
        val viewModel = viewModel(profiles)
        advanceUntilIdle()

        val state = assertIs<SettingsUiState.Error>(viewModel.state.value)
        assertIs<DomainError.Network>(state.error)
    }

    @Test
    fun `retrying after a failed load can reach Success`() = runTest {
        val profiles = FakeProfileRepository(name = "Alex Doyle", getError = DomainError.Network())
        val viewModel = viewModel(profiles)
        advanceUntilIdle()
        assertIs<SettingsUiState.Error>(viewModel.state.value)

        profiles.getError = null
        viewModel.onRetryLoad()
        advanceUntilIdle()

        assertIs<SettingsUiState.Success>(viewModel.state.value)
    }

    @Test
    fun `saving reports the new name`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onNameChange("Alexa Doyle")
        viewModel.onSave()
        assertEquals(true, (viewModel.state.value as SettingsUiState.Success).saving)
        advanceUntilIdle()

        val state = assertIs<SettingsUiState.Success>(viewModel.state.value)
        assertEquals("Alexa Doyle", state.name)
        assertEquals(false, state.saving)
        assertNull(state.saveError)
    }

    @Test
    fun `a blank name saves nothing`() = runTest {
        val profiles = FakeProfileRepository(name = "Alex Doyle")
        val viewModel = viewModel(profiles)
        advanceUntilIdle()

        viewModel.onNameChange("   ")
        viewModel.onSave()
        advanceUntilIdle()

        assertEquals("Alex Doyle", profiles.profile.name)
    }

    @Test
    fun `a second tap while saving is ignored`() = runTest {
        val profiles = FakeProfileRepository(name = "Alex Doyle")
        val viewModel = viewModel(profiles)
        advanceUntilIdle()

        viewModel.onNameChange("Alexa")
        viewModel.onSave()
        viewModel.onSave()
        advanceUntilIdle()

        assertEquals("Alexa", profiles.profile.name)
    }

    @Test
    fun `a failed save surfaces the error without losing the typed name`() = runTest {
        val profiles = FakeProfileRepository(name = "Alex Doyle", updateError = DomainError.Network())
        val viewModel = viewModel(profiles)
        advanceUntilIdle()

        viewModel.onNameChange("Alexa Doyle")
        viewModel.onSave()
        advanceUntilIdle()

        val state = assertIs<SettingsUiState.Success>(viewModel.state.value)
        assertIs<DomainError.Network>(state.saveError)
        assertEquals(false, state.saving)
        assertEquals("Alexa Doyle", state.name)
    }

    @Test
    fun `typing clears the save error`() = runTest {
        val profiles = FakeProfileRepository(name = "Alex Doyle", updateError = DomainError.Network())
        val viewModel = viewModel(profiles)
        advanceUntilIdle()

        viewModel.onNameChange("Alexa")
        viewModel.onSave()
        advanceUntilIdle()
        assertIs<DomainError.Network>((viewModel.state.value as SettingsUiState.Success).saveError)

        viewModel.onNameChange("Alexa D")
        assertNull((viewModel.state.value as SettingsUiState.Success).saveError)
    }

    @Test
    fun `the loaded name counts as unchanged`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue((viewModel.state.value as SettingsUiState.Success).isSameName)
    }

    @Test
    fun `typing makes the name changed`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onNameChange("Alexa Doyle")

        assertFalse((viewModel.state.value as SettingsUiState.Success).isSameName)
    }

    @Test
    fun `saving an unchanged name does nothing`() = runTest {
        // updateError would surface as saveError if updateName ran at all.
        val profiles = FakeProfileRepository(name = "Alex Doyle", updateError = DomainError.Network())
        val viewModel = viewModel(profiles)
        advanceUntilIdle()

        viewModel.onSave()
        advanceUntilIdle()

        val state = assertIs<SettingsUiState.Success>(viewModel.state.value)
        assertNull(state.saveError)
        assertEquals(false, state.saving)
    }

    @Test
    fun `typing the same name back counts as unchanged`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onNameChange("Alexa")
        viewModel.onNameChange("Alex Doyle")

        assertTrue((viewModel.state.value as SettingsUiState.Success).isSameName)
    }

    @Test
    fun `a successful save makes the name unchanged again`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onNameChange("Alexa Doyle")
        viewModel.onSave()
        advanceUntilIdle()

        val state = assertIs<SettingsUiState.Success>(viewModel.state.value)
        assertEquals("Alexa Doyle", state.savedName)
        assertTrue(state.isSameName)
    }

    @Test
    fun `a failed save leaves the name changed so it can be retried`() = runTest {
        val profiles = FakeProfileRepository(name = "Alex Doyle", updateError = DomainError.Network())
        val viewModel = viewModel(profiles)
        advanceUntilIdle()

        viewModel.onNameChange("Alexa Doyle")
        viewModel.onSave()
        advanceUntilIdle()

        assertFalse((viewModel.state.value as SettingsUiState.Success).isSameName)
    }

    @Test
    fun `a successful save emits saved once`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onNameChange("Alexa Doyle")
        viewModel.onSave()
        advanceUntilIdle()

        assertEquals(Unit, withTimeoutOrNull(1_000) { viewModel.saved.first() })
        assertNull(withTimeoutOrNull(1_000) { viewModel.saved.first() })
    }

    @Test
    fun `a failed save emits nothing`() = runTest {
        val profiles = FakeProfileRepository(name = "Alex Doyle", updateError = DomainError.Network())
        val viewModel = viewModel(profiles)
        advanceUntilIdle()

        viewModel.onNameChange("Alexa Doyle")
        viewModel.onSave()
        advanceUntilIdle()

        assertNull(withTimeoutOrNull(1_000) { viewModel.saved.first() })
    }
}
