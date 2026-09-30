package app.muster.ui.screens.settings

import app.muster.domain.error.DomainError

sealed interface DeleteAccountUiState {

    data object Hidden : DeleteAccountUiState

    data class Confirm(
        val deleting: Boolean = false,
        val error: DomainError? = null
    ) : DeleteAccountUiState

    data class SoleAdmin(
        val groupNames: List<String>,
        val deleting: Boolean = false,
        val error: DomainError? = null
    ) : DeleteAccountUiState
}
