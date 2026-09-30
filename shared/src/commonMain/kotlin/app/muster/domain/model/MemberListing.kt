package app.muster.domain.model

data class MemberListing(
    val members: List<Member>,
    val pendingInvitations: List<PendingInvitation>
)
