package app.muster.data.fake

import app.muster.domain.error.DomainError
import app.muster.domain.model.SessionState
import app.muster.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration.Companion.milliseconds

class FakeAuthRepository(initial: SessionState = SessionState.SignedOut) : AuthRepository {

    private val state = MutableStateFlow(initial)
    override val session: StateFlow<SessionState> = state.asStateFlow()

    override suspend fun requestSignInCode(email: String) {
        delay(FAKE_LATENCY_MS.milliseconds)
    }

    override suspend fun verifySignInCode(email: String, code: String) {
        delay(FAKE_LATENCY_MS.milliseconds)
        if (code == REJECTED_CODE) throw DomainError.InvalidCode()
        state.value = SessionState.SignedIn(userId = FAKE_USER_ID, email = email)
    }

    override suspend fun signOut() {
        state.value = SessionState.SignedOut
    }

    override suspend fun retrySession() {
        delay(FAKE_LATENCY_MS.milliseconds)
    }

    companion object {
        // Any other code signs in.
        const val REJECTED_CODE = "000000"
    }
}

internal const val FAKE_EMAIL = "alex.doyle@gmail.com"
internal const val FAKE_USER_ID = "00000000-0000-0000-0000-000000000001"
internal const val FAKE_LATENCY_MS = 600L
