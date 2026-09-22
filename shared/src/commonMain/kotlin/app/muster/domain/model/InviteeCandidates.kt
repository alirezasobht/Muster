package app.muster.domain.model

// Group members eligible for Add players
data class InviteeCandidates(
    val members: List<Member>,
    val capacity: Int,
    val freeSlots: Int,
    val queueLength: Int
)