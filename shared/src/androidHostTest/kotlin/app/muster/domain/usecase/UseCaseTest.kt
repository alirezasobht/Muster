package app.muster.domain.usecase

import app.muster.data.fake.FAKE_EMAIL
import app.muster.data.fake.FakeAuthRepository
import app.muster.data.fake.FakeProfileRepository
import app.muster.domain.model.SessionState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

// The use cases only delegate; the one thing they add is trimming, and a
// stray space from a keyboard would otherwise reach the API.
class UseCaseTest {

    @Test
    fun `requesting a code trims the address`() = runTest {
        val auth = FakeAuthRepository()
        RequestSignInCodeUseCase(auth)("  $FAKE_EMAIL  ")
        assertEquals(FAKE_EMAIL, auth.lastRequestedEmail)
    }

    @Test
    fun `updating a name trims it`() = runTest {
        val profiles = FakeProfileRepository()
        UpdateNameUseCase(profiles)("  Alex Doyle  ")
        assertEquals("Alex Doyle", profiles.profile.name)
    }

    @Test
    fun `observing the session exposes the repository flow`() = runTest {
        val auth = FakeAuthRepository(initial = SessionState.SignedOut)
        val session = ObserveSessionUseCase(auth)()
        assertEquals(SessionState.SignedOut, session.value)

        auth.emit(SessionState.SignedIn(userId = "id", email = FAKE_EMAIL))
        assertIs<SessionState.SignedIn>(session.value)
    }

    @Test
    fun `signing out clears the session`() = runTest {
        val auth = FakeAuthRepository(
            initial = SessionState.SignedIn(userId = "id", email = FAKE_EMAIL)
        )
        SignOutUseCase(auth)()
        assertEquals(SessionState.SignedOut, auth.session.value)
    }
}
