package app.muster.ui.screens.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.muster.domain.error.DomainError
import app.muster.domain.usecase.RequestSignInCodeUseCase
import app.muster.ui.common.ErrorPresentation
import app.muster.ui.common.presentation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RequestCodeViewModel(private val requestSignInCode: RequestSignInCodeUseCase) : ViewModel() {

    private val _state = MutableStateFlow(RequestCodeUiState())
    val state: StateFlow<RequestCodeUiState> = _state.asStateFlow()

    fun onEmailChange(email: String) {
        _state.update { it.copy(email = email, emailError = null, error = null) }
    }

    fun onSendCode() {
        val current = _state.value
        if (current.email.isBlank() || current.sending) return
        // Set before launching, not inside: the guard above must see it on a
        // second tap in the same frame, whatever dispatcher is in play.
        _state.update { it.copy(sending = true, emailError = null, error = null) }
        viewModelScope.launch {
            try {
                requestSignInCode(current.email)
                _state.update { it.copy(sending = false, sentTo = current.email.trim()) }
            } catch (e: DomainError) {
                _state.update {
                    when (e.presentation) {
                        ErrorPresentation.Field -> it.copy(sending = false, emailError = e)
                        is ErrorPresentation.Form -> it.copy(sending = false, error = e)
                    }
                }
            }
        }
    }

    // Called once the graph has navigated, so coming back doesn't navigate again.
    fun onSentToHandled() {
        _state.update { it.copy(sentTo = null) }
    }
}
