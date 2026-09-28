package app.muster.ui.screens.settings

import app.muster.domain.error.DomainError
import app.muster.ui.common.util.UiState

sealed interface SettingsUiState : UiState<SettingsUiState.Success> {

    data object Loading : SettingsUiState

    data class Success(
        val name: String = "",
        val savedName: String = "",
        val email: String = "",
        val saving: Boolean = false,
        val saveError: DomainError? = null
    ) : SettingsUiState {
        override fun asSuccessOrNull(): Success = this
        val isSameName: Boolean get() = name == savedName
    }

    data class Error(val error: DomainError) : SettingsUiState
}
