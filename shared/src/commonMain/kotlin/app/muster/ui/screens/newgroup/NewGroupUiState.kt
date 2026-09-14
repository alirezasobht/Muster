package app.muster.ui.screens.newgroup

import app.muster.domain.error.DomainError
import app.muster.domain.model.Group

data class NewGroupUiState(
    val name: String = "",
    val creating: Boolean = false,
    val error: DomainError? = null,
    val created: Group? = null
)
