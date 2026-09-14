package app.muster.domain.model

data class GroupInvitation(
    val id: String,
    val group: Group,
    val invitedByName: String?
)
