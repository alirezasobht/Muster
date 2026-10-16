package app.muster.domain.model

data class MemberTag(
    val id: String,
    val name: String,
    // In the admin's order.
    val memberIds: List<String>
)

data class TagLayoutEntry(
    val tagId: String,
    val memberIds: List<String>
)
