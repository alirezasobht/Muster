package app.muster.ui.screens.setname

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.muster.domain.error.DomainError
import app.muster.domain.model.Profile
import app.muster.domain.usecase.UpdateNameUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SetNameViewModel(
    private val updateName: UpdateNameUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SetNameUiState())
    val state: StateFlow<SetNameUiState> = _state.asStateFlow()

    fun onNameChange(name: String) {
        _state.update { it.copy(name = name, error = null) }
    }

    fun onContinue() {
        val current = _state.value
        if (current.name.isBlank() || current.saving) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            try {
                val profile = updateName(current.name)
                _state.update { it.copy(saving = false, saved = profile) }
            } catch (e: DomainError) {
                _state.update { it.copy(saving = false, error = e) }
            }
        }
    }

    // Call once routing has acted on `saved`. Resets so another account
    // signing in on this device does not start with this name filled in.
    fun onSavedHandled() {
        _state.value = SetNameUiState()
    }
}
