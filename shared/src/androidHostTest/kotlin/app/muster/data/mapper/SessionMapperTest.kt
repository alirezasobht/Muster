package app.muster.data.mapper

import app.muster.domain.model.SessionState
import io.github.jan.supabase.auth.status.RefreshFailureCause
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SessionMapperTest {

    private fun session(user: UserInfo?) = UserSession(
        accessToken = "access",
        refreshToken = "refresh",
        expiresIn = 3600,
        tokenType = "bearer",
        user = user
    )

    private fun user(id: String = "id-1", email: String? = "alex.doyle@gmail.com") =
        UserInfo(aud = "authenticated", id = id, email = email)

    @Test
    fun `initializing is loading`() {
        assertEquals(SessionState.Loading, SessionStatus.Initializing.toSessionState())
    }

    @Test
    fun `not authenticated is signed out`() {
        assertEquals(
            SessionState.SignedOut,
            SessionStatus.NotAuthenticated(isSignOut = true).toSessionState()
        )
    }

    // Offline with a stored session. Mapping this to SignedOut would drop the
    // user back to sign-in whenever their connection dropped.
    @Test
    fun `a refresh failure is unreachable, not signed out`() {
        val status = SessionStatus.RefreshFailure(
            RefreshFailureCause.NetworkError(RuntimeException("offline"))
        )
        assertEquals(SessionState.Unreachable, status.toSessionState())
    }

    @Test
    fun `an authenticated session carries the user`() {
        val status = SessionStatus.Authenticated(session(user()))
        val state = status.toSessionState()

        assertIs<SessionState.SignedIn>(state)
        assertEquals("id-1", state.userId)
        assertEquals("alex.doyle@gmail.com", state.email)
    }

    @Test
    fun `a missing email maps to empty rather than failing`() {
        val status = SessionStatus.Authenticated(session(user(email = null)))
        val state = status.toSessionState()

        assertIs<SessionState.SignedIn>(state)
        assertEquals("", state.email)
    }

    // The SDK types `user` as nullable. Treating that as signed in would
    // route to a home screen with no profile to load.
    @Test
    fun `an authenticated session with no user stays loading`() {
        val status = SessionStatus.Authenticated(session(user = null))
        assertEquals(SessionState.Loading, status.toSessionState())
    }
}
