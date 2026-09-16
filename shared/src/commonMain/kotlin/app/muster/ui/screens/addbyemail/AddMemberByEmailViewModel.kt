package app.muster.ui.screens.addbyemail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.muster.domain.error.DomainError
import app.muster.domain.usecase.InviteByEmailUseCase
import app.muster.ui.common.ErrorPresentation
import app.muster.ui.common.presentation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddMemberByEmailViewModel(
    private val groupId: String,
    private val inviteByEmail: InviteByEmailUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AddMemberByEmailUiState())
    val state: StateFlow<AddMemberByEmailUiState> = _state.asStateFlow()

    fun onEmailChange(email: String) {
        _state.update { it.copy(email = email, emailError = null, error = null) }
    }

    fun onInvite() {
        val current = _state.value
        if (current.email.isBlank() || current.sending) return
        _state.update { it.copy(sending = true, emailError = null, error = null) }
        viewModelScope.launch {
            try {
                inviteByEmail(groupId, current.email)
                _state.update { it.copy(sending = false, invited = true) }
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

    // Clears the field and stays on the form, dialog dismissed.
    fun onInviteMore() {
        _state.value = AddMemberByEmailUiState()
    }
}
