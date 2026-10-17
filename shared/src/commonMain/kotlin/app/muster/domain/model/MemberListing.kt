package app.muster.domain.model

data class MemberListing(
    val members: List<Member>,
    val pendingInvitations: List<PendingInvitation>,
    // Empty for non-admins: the server hides tags from them.
    val memberTags: List<MemberTag> = emptyList()
)
