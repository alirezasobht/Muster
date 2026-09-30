package app.muster.domain.model

data class Profile(
    val id: String,
    val name: String?,
    val email: String,
    val canCreateGroups: Boolean
)
