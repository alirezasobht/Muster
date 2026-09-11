package app.muster.domain.repository

import app.muster.domain.model.SessionState
import kotlinx.coroutines.flow.StateFlow

// Implementations must throw DomainError only; the UI catches nothing else.
interface AuthRepository {

    val session: StateFlow<SessionState>

    // Also creates the account if the address is new.
    suspend fun requestSignInCode(email: String)

    suspend fun verifySignInCode(email: String, code: String)

    suspend fun signOut()

    // Re-runs the stored-session check (1r's Try again after Unreachable).
    suspend fun retrySession()
}
