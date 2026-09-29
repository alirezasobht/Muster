package app.muster.data.mapper

import app.muster.domain.model.SessionState
import io.github.jan.supabase.auth.status.SessionStatus

internal fun SessionStatus.toSessionState(): SessionState = when (this) {
    SessionStatus.Initializing -> SessionState.Loading
    is SessionStatus.NotAuthenticated -> SessionState.SignedOut
    is SessionStatus.RefreshFailure -> SessionState.Unreachable
    is SessionStatus.Authenticated ->
        session.user
            ?.let { SessionState.SignedIn(userId = it.id, email = it.email.orEmpty()) }
            ?: SessionState.Loading
}
