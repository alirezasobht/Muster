package app.muster.ui.screens.launch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.muster.domain.error.DomainError
import app.muster.domain.model.Profile
import app.muster.domain.model.SessionState
import app.muster.domain.usecase.GetMyProfileUseCase
import app.muster.domain.usecase.ObserveSessionUseCase
import app.muster.domain.usecase.RetrySessionUseCase
import app.muster.domain.usecase.SignOutUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class LaunchViewModel(
    private val observeSession: ObserveSessionUseCase,
    private val getMyProfile: GetMyProfileUseCase,
    private val retrySession: RetrySessionUseCase,
    private val signOut: SignOutUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<LaunchUiState>(LaunchUiState.Loading)
    val state: StateFlow<LaunchUiState> = _state.asStateFlow()

    // The user whose screens are showing. While set, session changes for the
    // same user (token refreshes, Unreachable) must not replace the screen.
    private var routedUserId: String? = null
    private var signedInUserId: String? = null
    private var job: Job? = null

    private val started = TimeSource.Monotonic.markNow()
    private var firstResolution = true

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                when (session) {
                    SessionState.Loading ->
                        if (routedUserId == null) _state.value = LaunchUiState.Loading
                    SessionState.Unreachable ->
                        if (routedUserId == null) resolve(LaunchUiState.Failed)
                    SessionState.SignedOut -> {
                        job?.cancel()
                        routedUserId = null
                        signedInUserId = null
                        resolve(LaunchUiState.SignedOut)
                    }
                    is SessionState.SignedIn -> {
                        signedInUserId = session.userId
                        if (routedUserId != session.userId) loadProfile()
                    }
                }
            }
        }
    }

    fun onRetry() {
        if (observeSession().value == SessionState.Unreachable) retry() else loadProfile()
    }

    fun onNameSet(profile: Profile) {
        _state.value = LaunchUiState.Ready(profile)
    }

    fun onSignOut() {
        viewModelScope.launch {
            try {
                signOut()
            } catch (_: DomainError) {
                // Every failure path has already cleared the local session
                // (supabase-kt on server errors, AuthRepositoryImpl offline),
                // so the session flow still moves to SignedOut.
            }
        }
    }

    // A stored session usually resolves in well under a second, so without a
    // floor the launch screen appears and vanishes as a flicker. Applies to
    // the first resolution only — later sign-ins and sign-outs are immediate.
    private suspend fun resolve(next: LaunchUiState) {
        if (firstResolution) {
            firstResolution = false
            val remaining = MINIMUM_LAUNCH - started.elapsedNow()
            if (remaining.isPositive()) delay(remaining)
        }
        _state.value = next
    }

    private fun retry() {
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = LaunchUiState.Loading
            try {
                retrySession()
            } catch (_: DomainError) {
            }
            // Still Unreachable means the flow won't emit again, so nothing
            // else would take the screen off Loading.
            if (observeSession().value == SessionState.Unreachable) {
                _state.value = LaunchUiState.Failed
            }
        }
    }

    private fun loadProfile() {
        val userId = signedInUserId ?: return
        job?.cancel()
        job = viewModelScope.launch {
            // Coming from sign-in, 1b stays up with its spinner instead:
            // 1q is the launch screen, not a step between 1b and 1c.
            if (_state.value != LaunchUiState.SignedOut) _state.value = LaunchUiState.Loading
            try {
                val profile = getMyProfile()
                routedUserId = userId
                resolve(
                    if (profile.name == null) LaunchUiState.NeedsName else LaunchUiState.Ready(profile)
                )
            } catch (_: DomainError) {
                resolve(LaunchUiState.Failed)
            }
        }
    }

    private companion object {
        val MINIMUM_LAUNCH = 1.seconds
    }
}
