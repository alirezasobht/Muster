package app.muster.data.repository

import app.muster.data.dto.DeleteAccountDto
import app.muster.data.dto.SoleAdminGroupDto
import app.muster.data.mapper.mapErrors
import app.muster.data.mapper.toGroup
import app.muster.data.mapper.toSessionState
import app.muster.domain.model.Group
import app.muster.domain.model.SessionState
import app.muster.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.io.IOException

internal class AuthRepositoryImpl(private val client: SupabaseClient) : AuthRepository {

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

    override suspend fun verifySignInCode(
        email: String,
        code: String
    ) {
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

    override suspend fun deleteAccount(force: Boolean): List<Group> = mapErrors {
        val soleAdminGroups = client.postgrest.rpc(DELETE_ACCOUNT_FUNCTION, DeleteAccountDto(force))
            .decodeList<SoleAdminGroupDto>()
            .map { it.toGroup() }
        // Local only: the user is gone, so a server sign-out would be rejected.
        if (soleAdminGroups.isEmpty()) auth.clearSession()
        soleAdminGroups
    }

    private companion object {
        const val DELETE_ACCOUNT_FUNCTION = "delete_account"
    }
}
