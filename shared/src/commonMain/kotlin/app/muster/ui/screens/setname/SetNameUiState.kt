package app.muster.ui.screens.setname

import app.muster.domain.error.DomainError
import app.muster.domain.model.Profile

data class SetNameUiState(
    val name: String = "",
    val saving: Boolean = false,
    val error: DomainError? = null,
    val saved: Profile? = null
)
