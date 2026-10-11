package app.muster.ui.screens.editgroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.muster.domain.error.DomainError
import app.muster.domain.usecase.UpdateGroupUseCase
import app.muster.ui.common.ErrorPresentation
import app.muster.ui.common.presentation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditGroupViewModel(
    private val groupId: String,
    groupName: String,
    private val updateGroup: UpdateGroupUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(EditGroupUiState(initialName = groupName))
    val state: StateFlow<EditGroupUiState> = _state.asStateFlow()

    fun onNameChange(name: String) {
        _state.update { it.copy(name = name, nameError = null, error = null) }
    }

    fun onSave() {
        val current = _state.value
        if (!current.canSave || current.saving) return
        _state.update { it.copy(saving = true, nameError = null, error = null) }
        viewModelScope.launch {
            try {
                updateGroup(groupId, current.name)
                _state.update { it.copy(saving = false, saved = true) }
            } catch (e: DomainError) {
                _state.update {
                    when (e.presentation) {
                        ErrorPresentation.Field -> it.copy(saving = false, nameError = e)
                        is ErrorPresentation.Form -> it.copy(saving = false, error = e)
                    }
                }
            }
        }
    }

    fun onSavedHandled() {
        _state.update { it.copy(saved = false) }
    }
}
