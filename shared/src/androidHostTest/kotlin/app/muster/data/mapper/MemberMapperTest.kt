package app.muster.data.mapper

import app.muster.data.dto.GroupMembersDto
import app.muster.domain.model.GroupRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json

class MemberMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `RPC payload keeps tags in the order sent and each member's tag`() {
        val row = json.decodeFromString<List<GroupMembersDto>>(
            """
            [{
                "my_role": "admin",
                "members": [
                    {"profile_id": "a", "name": "Alex", "role": "admin", "tag_id": "t1"},
                    {"profile_id": "b", "name": null, "role": "member", "tag_id": "t1"},
                    {"profile_id": "c", "name": "Cam", "role": "member", "tag_id": null}
                ],
                "pending": [{"invitation_id": "i1", "email": "x@example.com"}],
                "tags": [
                    {"id": "t1", "name": "Regular"},
                    {"id": "t2", "name": "Casual"}
                ]
            }]
            """.trimIndent()
        ).single()

        val listing = row.toMemberListing()

        assertEquals(listOf("t1", "t2"), listing.memberTags.map { it.id })
        assertEquals(listOf("Regular", "Casual"), listing.memberTags.map { it.name })
        assertEquals(listOf("t1", "t1", null), listing.members.map { it.tagId })
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

    @Test
    fun `an unknown role maps to member`() {
        val row = json.decodeFromString<List<GroupMembersDto>>(
            """
            [{"members": [{"profile_id": "a", "name": "Alex", "role": "owner", "tag_id": null}],
              "pending": [], "tags": []}]
            """.trimIndent()
        ).single()

        assertEquals(GroupRole.Member, row.toMemberListing().members.single().role)
    }

    @Test
    fun `a member whose tag is not in the tags list keeps the tag id`() {
        val row = json.decodeFromString<List<GroupMembersDto>>(
            """
            [{"members": [{"profile_id": "a", "name": "Alex", "role": "member", "tag_id": "gone"}],
              "pending": [], "tags": [{"id": "t1", "name": "Regular"}]}]
            """.trimIndent()
        ).single()

        val listing = row.toMemberListing()

        assertEquals("gone", listing.members.single().tagId)
        assertEquals(listOf("t1"), listing.memberTags.map { it.id })
    }

    @Test
    fun `a member with no tag id key maps to no tag`() {
        val row = json.decodeFromString<List<GroupMembersDto>>(
            """
            [{"members": [{"profile_id": "a", "name": "Alex", "role": "member"}],
              "pending": [], "tags": []}]
            """.trimIndent()
        ).single()

        assertNull(row.toMemberListing().members.single().tagId)
    }

    @Test
    fun `a member with no name key maps to an empty name`() {
        val row = json.decodeFromString<List<GroupMembersDto>>(
            """[{"members": [{"profile_id": "a", "role": "member", "tag_id": null}], "pending": [], "tags": []}]"""
        ).single()

        assertEquals("", row.toMemberListing().members.single().name)
    }

    @Test
    fun `an empty payload maps to an empty listing`() {
        val row = json.decodeFromString<List<GroupMembersDto>>(
            """[{"members": [], "pending": [], "tags": []}]"""
        ).single()

        val listing = row.toMemberListing()

        assertEquals(emptyList(), listing.members)
        assertEquals(emptyList(), listing.pendingInvitations)
        assertEquals(emptyList(), listing.memberTags)
    }
}
