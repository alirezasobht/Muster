package app.muster.domain.model

sealed interface SessionState {

    data object Loading : SessionState

    data object SignedOut : SessionState

    data class SignedIn(val userId: String, val email: String) : SessionState

    // Signed in, but the access token has expired and refreshing it keeps
    // failing with a network error, so no request can succeed right now.
    // supabase-kt refreshes at ~80% of the token's 1-hour life and retries
    // every 10 s; this state appears only once the token is actually past
    // expiry. The session stays stored and returns to SignedIn on its own
    // when the network does.
    //
    // Happens at launch (opened offline with a stale token) and mid-session
    // (offline past expiry, or resumed from a long background).
    //
    // Never route this to sign-in: the user is not signed out, and a new code
    // could not arrive offline anyway. Mid-session it must not replace the
    // current screen either. A revoked refresh token is a different case: the
    // SDK clears the session and this becomes SignedOut.
    data object Unreachable : SessionState
}
