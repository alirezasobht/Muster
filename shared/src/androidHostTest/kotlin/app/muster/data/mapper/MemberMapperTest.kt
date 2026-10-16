package app.muster.data.mapper

import app.muster.data.dto.GroupMembersDto
import app.muster.domain.model.GroupRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

class MemberMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `RPC payload keeps tags and their members in the order sent`() {
        val row = json.decodeFromString<List<GroupMembersDto>>(
            """
            [{
                "my_role": "admin",
                "members": [
                    {"profile_id": "a", "name": "Alex", "role": "admin"},
                    {"profile_id": "b", "name": null, "role": "member"},
                    {"profile_id": "c", "name": "Cam", "role": "member"}
                ],
                "pending": [{"invitation_id": "i1", "email": "x@example.com"}],
                "tags": [
                    {"id": "t1", "name": "Regular", "position": 1, "members": ["b", "a"]},
                    {"id": "t2", "name": "Casual", "position": 2, "members": []}
                ]
            }]
            """.trimIndent()
        ).single()

        val listing = row.toMemberListing()

        assertEquals(listOf("t1", "t2"), listing.memberTags.map { it.id })
        assertEquals(listOf("b", "a"), listing.memberTags.first().memberIds)
        assertEquals(emptyList(), listing.memberTags.last().memberIds)
        assertEquals("", listing.members[1].name)
        assertEquals(GroupRole.Admin, listing.members.first().role)
        assertEquals("i1", listing.pendingInvitations.single().id)
    }

    @Test
    fun `a member's payload has no tags`() {
        val row = json.decodeFromString<List<GroupMembersDto>>(
            """[{"my_role": "member", "members": [], "pending": [], "tags": []}]"""
        ).single()

        assertEquals(emptyList(), row.toMemberListing().memberTags)
    }
}
