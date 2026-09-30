package app.muster.domain.model

data class Member(
    val profileId: String,
    val name: String,
    val role: GroupRole
)
