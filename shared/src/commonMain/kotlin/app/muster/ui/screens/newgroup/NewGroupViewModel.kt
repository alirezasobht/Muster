package app.muster.ui.screens.newgroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.muster.domain.error.DomainError
import app.muster.domain.usecase.CreateGroupUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NewGroupViewModel(
    private val createGroup: CreateGroupUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(NewGroupUiState())
    val state: StateFlow<NewGroupUiState> = _state.asStateFlow()

    fun onNameChange(name: String) {
        _state.update { it.copy(name = name, error = null) }
    }

    fun onCreate() {
        val current = _state.value
        if (current.name.isBlank() || current.creating) return
        // Set before launching, not inside: the guard above must see it on a
        // second tap in the same frame, whatever dispatcher is in play.
        _state.update { it.copy(creating = true, error = null) }
        viewModelScope.launch {
            try {
                val group = createGroup(current.name)
                _state.update { it.copy(creating = false, created = group) }
            } catch (e: DomainError) {
                _state.update { it.copy(creating = false, error = e) }
            }
        }
    }

    // Call once routing has acted on `created`, so returning to this screen
    // does not navigate again.
    fun onCreatedHandled() {
        _state.value = NewGroupUiState()
    }
}
