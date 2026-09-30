package app.muster.ui.screens.launch

import app.muster.data.fake.FAKE_EMAIL
import app.muster.data.fake.FAKE_USER_ID
import app.muster.data.fake.FakeAuthRepository
import app.muster.data.fake.FakeProfileRepository
import app.muster.domain.error.DomainError
import app.muster.domain.model.SessionState
import app.muster.domain.usecase.GetMyProfileUseCase
import app.muster.domain.usecase.ObserveSessionUseCase
import app.muster.domain.usecase.RetrySessionUseCase
import app.muster.domain.usecase.SignOutUseCase
import app.muster.testing.MainDispatcherRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class LaunchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val signedIn = SessionState.SignedIn(userId = FAKE_USER_ID, email = FAKE_EMAIL)

    private fun viewModel(
        auth: FakeAuthRepository = FakeAuthRepository(),
        profiles: FakeProfileRepository = FakeProfileRepository()
    ) = LaunchViewModel(
        observeSession = ObserveSessionUseCase(auth),
        getMyProfile = GetMyProfileUseCase(profiles),
        retrySession = RetrySessionUseCase(auth),
        signOut = SignOutUseCase(auth)
    )

    @Test
    fun `starts loading`() {
        assertEquals(LaunchUiState.Loading, viewModel().state.value)
    }

    // The launch screen is held for a second so it does not flash past.
    @Test
    fun `resolving is held for a second`() = runTest {
        val viewModel = viewModel(FakeAuthRepository(initial = SessionState.SignedOut))
        advanceTimeBy(500.milliseconds)
        assertEquals(LaunchUiState.Loading, viewModel.state.value)

        advanceUntilIdle()
        assertEquals(LaunchUiState.SignedOut, viewModel.state.value)
    }

    @Test
    fun `a signed-in session with a name goes home`() = runTest {
        val viewModel = viewModel(
            auth = FakeAuthRepository(initial = signedIn),
            profiles = FakeProfileRepository(name = "Alex Doyle")
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertIs<LaunchUiState.Ready>(state)
        assertEquals("Alex Doyle", state.profile.name)
    }

    @Test
    fun `a signed-in session without a name asks for one`() = runTest {
        val viewModel = viewModel(
            auth = FakeAuthRepository(initial = signedIn),
            profiles = FakeProfileRepository(name = null)
        )
        advanceUntilIdle()

        assertEquals(LaunchUiState.NeedsName, viewModel.state.value)
    }

    @Test
    fun `a failed profile load fails the launch`() = runTest {
        val viewModel = viewModel(
            auth = FakeAuthRepository(initial = signedIn),
            profiles = FakeProfileRepository(getError = DomainError.Network())
        )
        advanceUntilIdle()

        assertEquals(LaunchUiState.Failed, viewModel.state.value)
    }

    // Offline with a stored session: still signed in, just unreachable.
    @Test
    fun `an unreachable session fails the launch rather than signing out`() = runTest {
        val viewModel = viewModel(FakeAuthRepository(initial = SessionState.Unreachable))
        advanceUntilIdle()

        assertEquals(LaunchUiState.Failed, viewModel.state.value)
    }

    @Test
    fun `retrying after a failed profile load recovers`() = runTest {
        val profiles = FakeProfileRepository(name = "Alex", getError = DomainError.Network())
        val viewModel = viewModel(auth = FakeAuthRepository(initial = signedIn), profiles = profiles)
        advanceUntilIdle()
        assertEquals(LaunchUiState.Failed, viewModel.state.value)

        profiles.getError = null
        viewModel.onRetry()
        advanceUntilIdle()

        assertIs<LaunchUiState.Ready>(viewModel.state.value)
    }

    @Test
    fun `retrying while still unreachable stays failed`() = runTest {
        val auth = FakeAuthRepository(initial = SessionState.Unreachable)
        val viewModel = viewModel(auth)
        advanceUntilIdle()

        viewModel.onRetry()
        advanceUntilIdle()

        assertEquals(LaunchUiState.Failed, viewModel.state.value)
    }

    @Test
    fun `the session recovering on its own goes home`() = runTest {
        val auth = FakeAuthRepository(initial = SessionState.Unreachable)
        val viewModel = viewModel(auth, FakeProfileRepository(name = "Alex"))
        advanceUntilIdle()
        assertEquals(LaunchUiState.Failed, viewModel.state.value)

        auth.emit(signedIn)
        advanceUntilIdle()

        assertIs<LaunchUiState.Ready>(viewModel.state.value)
    }

    // Coming from sign-in the state is SignedOut, and 1b must stay up with
    // its spinner rather than flashing the green launch screen.
    @Test
    fun `signing in does not show the launch screen again`() = runTest {
        val auth = FakeAuthRepository(initial = SessionState.SignedOut)
        val viewModel = viewModel(auth, FakeProfileRepository(name = "Alex"))
        advanceUntilIdle()
        assertEquals(LaunchUiState.SignedOut, viewModel.state.value)

        auth.emit(signedIn)
        // Mid-load: still SignedOut, never Loading.
        advanceTimeBy(100.milliseconds)
        assertEquals(LaunchUiState.SignedOut, viewModel.state.value)

        advanceUntilIdle()
        assertIs<LaunchUiState.Ready>(viewModel.state.value)
    }

    // The one-second floor is for launch only; a later sign-out is immediate.
    @Test
    fun `signing out is not delayed`() = runTest {
        val auth = FakeAuthRepository(initial = signedIn)
        val viewModel = viewModel(auth, FakeProfileRepository(name = "Alex"))
        advanceUntilIdle()

        viewModel.onSignOut()
        advanceUntilIdle()

        assertEquals(LaunchUiState.SignedOut, viewModel.state.value)
    }

    // supabase-kt clears the local session even when the call fails, so the
    // session flow still reports a sign-out.
    @Test
    fun `a failed sign-out still signs out`() = runTest {
        val auth = FakeAuthRepository(initial = signedIn, signOutError = DomainError.Network())
        val viewModel = viewModel(auth, FakeProfileRepository(name = "Alex"))
        advanceUntilIdle()

        viewModel.onSignOut()
        advanceUntilIdle()
        auth.emit(SessionState.SignedOut)
        advanceUntilIdle()

        assertEquals(LaunchUiState.SignedOut, viewModel.state.value)
    }

    @Test
    fun `setting a name goes home without reloading`() = runTest {
        val profiles = FakeProfileRepository(name = null)
        val viewModel = viewModel(FakeAuthRepository(initial = signedIn), profiles)
        advanceUntilIdle()
        assertEquals(LaunchUiState.NeedsName, viewModel.state.value)

        val named = profiles.profile.copy(name = "Alex Doyle")
        viewModel.onNameSet(named)

        val state = viewModel.state.value
        assertIs<LaunchUiState.Ready>(state)
        assertEquals("Alex Doyle", state.profile.name)
    }
}
