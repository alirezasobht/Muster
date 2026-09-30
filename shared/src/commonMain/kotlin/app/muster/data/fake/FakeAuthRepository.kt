package app.muster.data.fake

import app.muster.domain.error.DomainError
import app.muster.domain.model.Group
import app.muster.domain.model.SessionState
import app.muster.domain.repository.AuthRepository
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAuthRepository(
    initial: SessionState = SessionState.SignedOut,
    // Set to make the next call of that kind throw instead of succeeding.
    // Stays set until cleared, so a test can drive repeated failures.
    var requestError: DomainError? = null,
    var verifyError: DomainError? = null,
    var signOutError: DomainError? = null,
    var retryError: DomainError? = null,
    var deleteError: DomainError? = null,
    // Returned by a non-forced delete; empty means the delete goes through.
    var soleAdminGroups: List<Group> = emptyList(),
    private val latency: Long = FAKE_LATENCY_MS
) : AuthRepository {

    private val state = MutableStateFlow(initial)
    override val session: StateFlow<SessionState> = state.asStateFlow()

    var requestedCodes: Int = 0
        private set

    var lastRequestedEmail: String? = null
        private set

    var deleteCalls: Int = 0
        private set

    override suspend fun requestSignInCode(email: String) {
        delay(latency.milliseconds)
        requestError?.let { throw it }
        lastRequestedEmail = email
        requestedCodes++
    }

    override suspend fun verifySignInCode(
        email: String,
        code: String
    ) {
        delay(latency.milliseconds)
        verifyError?.let { throw it }
        if (code == REJECTED_CODE) throw DomainError.InvalidCode()
        state.value = SessionState.SignedIn(userId = FAKE_USER_ID, email = email)
    }

    override suspend fun signOut() {
        signOutError?.let { throw it }
        state.value = SessionState.SignedOut
    }

    override suspend fun retrySession() {
        delay(latency.milliseconds)
        retryError?.let { throw it }
    }

    override suspend fun deleteAccount(force: Boolean): List<Group> {
        deleteCalls++
        delay(latency.milliseconds)
        deleteError?.let { throw it }
        if (!force && soleAdminGroups.isNotEmpty()) return soleAdminGroups
        state.value = SessionState.SignedOut
        return emptyList()
    }

    // Stands in for the SDK reaching the server again, or losing it.
    fun emit(session: SessionState) {
        state.value = session
    }

    companion object {
        // Any other code signs in.
        const val REJECTED_CODE = "000000"
    }
}

const val FAKE_EMAIL = "alex.doyle@gmail.com"
const val FAKE_USER_ID = "00000000-0000-0000-0000-000000000001"
const val FAKE_LATENCY_MS = 600L
