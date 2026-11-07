package app.muster.ui.screens.auth.setname

import app.muster.domain.error.DomainError
import app.muster.domain.model.Profile

data class SetNameUiState(
    val name: String = "",
    val saving: Boolean = false,
    val error: DomainError? = null,
    val saved: Profile? = null
)
