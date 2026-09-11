package app.muster.data.repository

import app.muster.data.mapper.mapErrors
import app.muster.data.mapper.toSessionState
import app.muster.domain.model.SessionState
import app.muster.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.io.IOException

internal class AuthRepositoryImpl(client: SupabaseClient) : AuthRepository {

    private val auth = client.auth
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val session: StateFlow<SessionState> = auth.sessionStatus
        .map { it.toSessionState() }
        .stateIn(scope, SharingStarted.Eagerly, auth.sessionStatus.value.toSessionState())

    override suspend fun requestSignInCode(email: String) = mapErrors {
        auth.signInWith(OTP) {
            this.email = email
            createUser = true
        }
    }

    override suspend fun verifySignInCode(email: String, code: String) {
        mapErrors {
            auth.verifyEmailOtp(type = OtpType.Email.EMAIL, email = email, token = code)
        }
    }

    override suspend fun signOut() = mapErrors {
        try {
            auth.signOut()
        } catch (e: IOException) {
            // Offline, supabase-kt throws before clearing local data, which
            // would leave the user unable to sign out. Clear it locally; the
            // server-side session is orphaned, and nothing can use it.
            auth.clearSession()
        }
    }

    override suspend fun retrySession() {
        mapErrors { auth.loadFromStorage() }
    }
}
