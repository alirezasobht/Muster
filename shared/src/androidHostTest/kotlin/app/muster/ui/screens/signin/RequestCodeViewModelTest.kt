package app.muster.ui.screens.signin

import app.muster.data.fake.FAKE_EMAIL
import app.muster.data.fake.FakeAuthRepository
import app.muster.domain.error.DomainError
import app.muster.domain.usecase.RequestSignInCodeUseCase
import app.muster.testing.MainDispatcherRule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RequestCodeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(auth: FakeAuthRepository = FakeAuthRepository()) =
        RequestCodeViewModel(RequestSignInCodeUseCase(auth))

    @Test
    fun `starts empty`() {
        val viewModel = viewModel()
        assertEquals(RequestCodeUiState(), viewModel.state.value)
    }

    @Test
    fun `typing clears both errors`() = runTest {
        val auth = FakeAuthRepository(requestError = DomainError.Network())
        val viewModel = viewModel(auth)
        viewModel.onEmailChange(FAKE_EMAIL)
        viewModel.onSendCode()
        advanceUntilIdle()
        assertIs<DomainError.Network>(viewModel.state.value.error)

        viewModel.onEmailChange("a")
        assertNull(viewModel.state.value.error)
        assertNull(viewModel.state.value.emailError)
    }

    @Test
    fun `sending a code reports the trimmed address`() = runTest {
        val viewModel = viewModel()
        viewModel.onEmailChange("  $FAKE_EMAIL  ")
        viewModel.onSendCode()

        assertTrue(viewModel.state.value.sending)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(false, state.sending)
        assertEquals(FAKE_EMAIL, state.sentTo)
        assertNull(state.error)
    }

    @Test
    fun `a blank address sends nothing`() = runTest {
        val auth = FakeAuthRepository()
        val viewModel = viewModel(auth)
        viewModel.onEmailChange("   ")
        viewModel.onSendCode()
        advanceUntilIdle()

        assertEquals(0, auth.requestedCodes)
        assertNull(viewModel.state.value.sentTo)
    }

    @Test
    fun `a second tap while sending is ignored`() = runTest {
        val auth = FakeAuthRepository()
        val viewModel = viewModel(auth)
        viewModel.onEmailChange(FAKE_EMAIL)
        viewModel.onSendCode()
        viewModel.onSendCode()
        advanceUntilIdle()

        assertEquals(1, auth.requestedCodes)
    }

    // The split matters: a bad address belongs under the field, a network
    // failure does not.
    @Test
    fun `an invalid address is a field error`() = runTest {
        val auth = FakeAuthRepository(requestError = DomainError.InvalidEmail())
        val viewModel = viewModel(auth)
        viewModel.onEmailChange("not-an-email")
        viewModel.onSendCode()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertIs<DomainError.InvalidEmail>(state.emailError)
        assertNull(state.error)
        assertNull(state.sentTo)
    }

    @Test
    fun `everything else is a screen error`() = runTest {
        val auth = FakeAuthRepository(requestError = DomainError.RateLimited())
        val viewModel = viewModel(auth)
        viewModel.onEmailChange(FAKE_EMAIL)
        viewModel.onSendCode()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertIs<DomainError.RateLimited>(state.error)
        assertNull(state.emailError)
        assertEquals(false, state.sending)
    }

    // Without this the graph would navigate again every time the screen
    // recomposes after coming back from 1b.
    @Test
    fun `sentTo clears once handled`() = runTest {
        val viewModel = viewModel()
        viewModel.onEmailChange(FAKE_EMAIL)
        viewModel.onSendCode()
        advanceUntilIdle()
        assertEquals(FAKE_EMAIL, viewModel.state.value.sentTo)

        viewModel.onSentToHandled()
        assertNull(viewModel.state.value.sentTo)
    }
}
