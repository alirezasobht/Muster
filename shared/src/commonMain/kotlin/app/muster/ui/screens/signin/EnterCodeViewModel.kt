package app.muster.ui.screens.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.muster.domain.error.DomainError
import app.muster.domain.usecase.RequestSignInCodeUseCase
import app.muster.domain.usecase.VerifySignInCodeUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class EnterCodeViewModel(
    email: String,
    private val requestSignInCode: RequestSignInCodeUseCase,
    private val verifySignInCode: VerifySignInCodeUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(EnterCodeUiState(email = email))
    val state: StateFlow<EnterCodeUiState> = _state.asStateFlow()

    private var countdown: Job? = null

    init {
        // A code was just sent to get here.
        startCountdown()
    }

    fun onCodeChange(code: String) {
        _state.update { it.copy(code = code, error = null) }
    }

    fun onVerify() {
        val current = _state.value
        if (current.code.length != CODE_LENGTH || current.verifying) return
        // Set before launching, not inside: the guard above must see it on a
        // second tap in the same frame, whatever dispatcher is in play.
        _state.update { it.copy(verifying = true, error = null) }
        viewModelScope.launch {
            try {
                verifySignInCode(current.email, current.code)
                // Leave `verifying` set: the session flow drives the next
                // screen, and this one stays up with its spinner until then.
                countdown?.cancel()
            } catch (e: DomainError) {
                _state.update { it.copy(verifying = false, code = "", error = e) }
            }
        }
    }

    fun onResend() {
        val current = _state.value
        if (current.resendInSeconds > 0 || current.verifying) return
        viewModelScope.launch {
            try {
                requestSignInCode(current.email)
                _state.update { it.copy(code = "", error = null) }
                startCountdown()
            } catch (e: DomainError) {
                _state.update { it.copy(error = e) }
            }
        }
    }

    private fun startCountdown() {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            for (seconds in RESEND_SECONDS downTo 1) {
                _state.update { it.copy(resendInSeconds = seconds) }
                delay(1.seconds)
            }
            _state.update { it.copy(resendInSeconds = 0) }
        }
    }

    private companion object {
        // Supabase's minimum interval between codes to one address. Shorter
        // and an early resend fails with RateLimited.
        const val RESEND_SECONDS = 60
    }
}
