package app.muster.ui.screens.signin

import app.muster.data.fake.FAKE_EMAIL
import app.muster.data.fake.FakeAuthRepository
import app.muster.domain.error.DomainError
import app.muster.domain.model.SessionState
import app.muster.domain.usecase.RequestSignInCodeUseCase
import app.muster.domain.usecase.VerifySignInCodeUseCase
import app.muster.testing.MainDispatcherRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class EnterCodeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(auth: FakeAuthRepository = FakeAuthRepository()) = EnterCodeViewModel(
        email = FAKE_EMAIL,
        requestSignInCode = RequestSignInCodeUseCase(auth),
        verifySignInCode = VerifySignInCodeUseCase(auth)
    )

    @Test
    fun `keeps the address it was created with`() {
        assertEquals(FAKE_EMAIL, viewModel().state.value.email)
    }

    // A code was already sent to reach this screen, so the countdown must be
    // running before the user does anything.
    @Test
    fun `the resend countdown starts immediately`() = runTest {
        val viewModel = viewModel()
        advanceTimeBy(1.seconds)
        assertEquals(60, viewModel.state.value.resendInSeconds)
    }

    @Test
    fun `the countdown reaches zero after a minute`() = runTest {
        val viewModel = viewModel()
        advanceTimeBy(61.seconds)
        assertEquals(0, viewModel.state.value.resendInSeconds)
    }

    @Test
    fun `typing clears both errors`() = runTest {
        val viewModel = viewModel()
        viewModel.onCodeChange(FakeAuthRepository.REJECTED_CODE)
        viewModel.onVerify()
        advanceUntilIdle()
        assertIs<DomainError.InvalidCode>(viewModel.state.value.codeError)

        viewModel.onCodeChange("1")
        assertNull(viewModel.state.value.codeError)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `a short code does not verify`() = runTest {
        val auth = FakeAuthRepository()
        val viewModel = viewModel(auth)
        viewModel.onCodeChange("123")
        viewModel.onVerify()
        advanceUntilIdle()

        assertEquals(SessionState.SignedOut, auth.session.value)
        assertEquals(false, viewModel.state.value.verifying)
    }

    @Test
    fun `a correct code signs in`() = runTest {
        val auth = FakeAuthRepository()
        val viewModel = viewModel(auth)
        viewModel.onCodeChange("418027")
        viewModel.onVerify()
        assertTrue(viewModel.state.value.verifying)
        advanceUntilIdle()

        assertIs<SessionState.SignedIn>(auth.session.value)
        assertNull(viewModel.state.value.codeError)
        assertNull(viewModel.state.value.error)
    }

    // Routing reacts to the session, and this screen stays up with its
    // spinner until it does. Clearing `verifying` here would flash an
    // enabled Continue button first.
    @Test
    fun `verifying stays set after success`() = runTest {
        val viewModel = viewModel()
        viewModel.onCodeChange("418027")
        viewModel.onVerify()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.verifying)
    }

    @Test
    fun `a wrong code clears the boxes and reports it under the field`() = runTest {
        val viewModel = viewModel()
        viewModel.onCodeChange(FakeAuthRepository.REJECTED_CODE)
        viewModel.onVerify()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertIs<DomainError.InvalidCode>(state.codeError)
        assertNull(state.error)
        assertEquals("", state.code)
        assertEquals(false, state.verifying)
    }

    @Test
    fun `a rate limit goes above the button, not under the field`() = runTest {
        val auth = FakeAuthRepository(verifyError = DomainError.RateLimited())
        val viewModel = viewModel(auth)
        viewModel.onCodeChange("418027")
        viewModel.onVerify()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertIs<DomainError.RateLimited>(state.error)
        assertNull(state.codeError)
    }

    // Terminal: retrying the same request fails the same way.
    @Test
    fun `a rate limit disables the button`() = runTest {
        val auth = FakeAuthRepository(verifyError = DomainError.RateLimited())
        val viewModel = viewModel(auth)
        viewModel.onCodeChange("418027")
        viewModel.onVerify()
        advanceUntilIdle()

        assertEquals(false, viewModel.state.value.canVerify)
    }

    @Test
    fun `typing again re-enables the button after a rate limit`() = runTest {
        val auth = FakeAuthRepository(verifyError = DomainError.RateLimited())
        val viewModel = viewModel(auth)
        viewModel.onCodeChange("418027")
        viewModel.onVerify()
        advanceUntilIdle()

        viewModel.onCodeChange("418027")

        assertTrue(viewModel.state.value.canVerify)
    }

    @Test
    fun `an offline failure leaves the button live`() = runTest {
        val auth = FakeAuthRepository(verifyError = DomainError.Network())
        val viewModel = viewModel(auth)
        viewModel.onCodeChange("418027")
        viewModel.onVerify()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertIs<DomainError.Network>(state.error)
        // The code is cleared on failure, so canVerify is false on length —
        // what matters is that the error itself is not terminal.
        viewModel.onCodeChange("418027")
        assertTrue(viewModel.state.value.canVerify)
    }

    @Test
    fun `resend is refused while the countdown runs`() = runTest {
        val auth = FakeAuthRepository()
        val viewModel = viewModel(auth)
        advanceTimeBy(5.seconds)
        viewModel.onResend()
        advanceUntilIdle()

        assertEquals(0, auth.requestedCodes)
    }

    @Test
    fun `resend works once the countdown ends and restarts it`() = runTest {
        val auth = FakeAuthRepository()
        val viewModel = viewModel(auth)
        advanceTimeBy(61.seconds)
        viewModel.onResend()
        // Not advanceUntilIdle: that would drain the whole new countdown too.
        advanceTimeBy(700.milliseconds)

        assertEquals(1, auth.requestedCodes)
        assertEquals(60, viewModel.state.value.resendInSeconds)
    }

    @Test
    fun `a failed resend surfaces the error above the button`() = runTest {
        val auth = FakeAuthRepository(requestError = DomainError.RateLimited())
        val viewModel = viewModel(auth)
        advanceTimeBy(61.seconds)
        viewModel.onResend()
        advanceUntilIdle()

        assertIs<DomainError.RateLimited>(viewModel.state.value.error)
        assertNull(viewModel.state.value.codeError)
    }
}
