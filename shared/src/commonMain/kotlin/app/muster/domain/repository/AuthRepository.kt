package app.muster.domain.repository

import app.muster.domain.model.Group
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

    // Without force, returns the groups the caller is the only admin of and
    // deletes nothing if there are any. Otherwise archives them, deletes the
    // account, signs out locally and returns an empty list.
    suspend fun deleteAccount(force: Boolean): List<Group>
}
