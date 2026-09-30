package app.muster.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.muster.domain.error.DomainError
import app.muster.domain.usecase.DeleteAccountUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeleteAccountViewModel(private val deleteAccount: DeleteAccountUseCase) : ViewModel() {

    private val _state = MutableStateFlow<DeleteAccountUiState>(DeleteAccountUiState.Hidden)
    val state: StateFlow<DeleteAccountUiState> = _state.asStateFlow()

    fun onStart() {
        _state.value = DeleteAccountUiState.Confirm()
    }

    fun onDismiss() {
        if (!isDeleting()) _state.value = DeleteAccountUiState.Hidden
    }

    fun onConfirm() {
        when (val current = _state.value) {
            is DeleteAccountUiState.Confirm -> if (!current.deleting) softDelete()
            is DeleteAccountUiState.SoleAdmin -> if (!current.deleting) forceDelete(current)
            is DeleteAccountUiState.Hidden -> Unit
        }
    }

    private fun softDelete() {
        _state.value = DeleteAccountUiState.Confirm(deleting = true)
        viewModelScope.launch {
            try {
                val soleAdminGroups = deleteAccount(force = false)
                // Success with empty soleAdminGroups needs no state: the cleared session reaches LaunchViewModel's
                // session collector as SignedOut, and NavGraph resets to sign-in.
                if (soleAdminGroups.isNotEmpty()) {
                    _state.value = DeleteAccountUiState.SoleAdmin(groupNames = soleAdminGroups.map { it.name })
                }
            } catch (e: DomainError) {
                _state.value = DeleteAccountUiState.Confirm(error = e)
            }
        }
    }

    private fun forceDelete(current: DeleteAccountUiState.SoleAdmin) {
        _state.value = current.copy(deleting = true, error = null)
        viewModelScope.launch {
            try {
                // Success needs no state: the cleared session reaches LaunchViewModel's
                // session collector as SignedOut, and NavGraph resets to sign-in.
                deleteAccount(force = true)
            } catch (e: DomainError) {
                _state.value = current.copy(deleting = false, error = e)
            }
        }
    }

    private fun isDeleting(): Boolean = when (val current = _state.value) {
        is DeleteAccountUiState.Confirm -> current.deleting
        is DeleteAccountUiState.SoleAdmin -> current.deleting
        is DeleteAccountUiState.Hidden -> false
    }
}
