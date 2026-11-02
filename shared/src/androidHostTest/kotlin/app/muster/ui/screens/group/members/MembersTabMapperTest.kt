package app.muster.ui.screens.group.members

import app.muster.domain.error.DomainError
import app.muster.domain.model.GroupRole
import app.muster.domain.model.Member
import app.muster.domain.model.MemberListing
import app.muster.domain.model.MemberTag
import app.muster.domain.model.PendingInvitation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MembersTabMapperTest {

    private fun member(
        id: String,
        name: String,
        role: GroupRole = GroupRole.Member,
        tagId: String? = null
    ) = Member(profileId = id, name = name, role = role, tagId = tagId)

    private fun listing(
        members: List<Member> = emptyList(),
        pendings: List<PendingInvitation> = emptyList(),
        tags: List<MemberTag> = emptyList()
    ) = MemberListing(members = members, pendingInvitations = pendings, memberTags = tags)

    private fun row(
        id: String,
        name: String,
        status: MemberStatus = MemberStatus.Member,
        isSelf: Boolean = false
    ) = MemberRow(id = id, displayName = name, status = status, isSelf = isSelf)

    private val fullListing = listing(
        members = listOf(
            member("me", "Zed", GroupRole.Admin),
            member("a1", "bob", GroupRole.Admin, tagId = "t1"),
            member("m1", "Carl", tagId = "t1"),
            member("m2", "alice", tagId = "t2"),
            member("m3", "Dave"),
            member("m4", "Eve", tagId = "")
        ),
        pendings = listOf(
            PendingInvitation("p1", "zoe@example.com"),
            PendingInvitation("p2", "Adam@example.com")
        ),
        tags = listOf(MemberTag("t1", "Defenders"), MemberTag("t2", "Forwards"))
    )

    private fun adminState() = fullListing.toSuccessState(myProfileId = "me", isAdmin = true)

    @Test
    fun `an admin state has tags untagged members pendings and the admin flags`() {
        val state = adminState()

        assertEquals(listOf("t1", "t2"), state.tags.map { it.id })
        assertEquals(listOf("Defenders", "Forwards"), state.tags.map { it.name })
        assertEquals(listOf("me", "m3", "m4"), state.members.map { it.id })
        assertEquals(listOf("p2", "p1"), state.pendings.map { it.id })
        assertTrue(state.canAddMembers)
        assertTrue(state.canManageTags)
        assertFalse(state.isRefreshing)
        assertNull(state.actionTargetId)
        assertNull(state.movingProfileId)
        assertNull(state.actionError)
    }

    @Test
    fun `a non-admin state has all members and no tags or pendings`() {
        val state = fullListing.toSuccessState(myProfileId = "m3", isAdmin = false)

        assertTrue(state.tags.isEmpty())
        assertTrue(state.pendings.isEmpty())
        assertEquals(6, state.members.size)
        assertFalse(state.canAddMembers)
        assertFalse(state.canManageTags)
    }

    @Test
    fun `non-admin rows have no action flags`() {
        val state = fullListing.toSuccessState(myProfileId = "m3", isAdmin = false)

        state.members.forEach {
            assertFalse(it.canPromote)
            assertFalse(it.canDemote)
            assertFalse(it.canRemove)
            assertFalse(it.canMoveToTag)
            assertFalse(it.hasMenu)
        }
    }

    @Test
    fun `a non-admin keeps tag ids on rows`() {
        val state = fullListing.toSuccessState(myProfileId = "m3", isAdmin = false)

        assertEquals("t1", state.members.first { it.id == "a1" }.tagId)
    }

    @Test
    fun `a non-admin self row is marked as self`() {
        val state = fullListing.toSuccessState(myProfileId = "m3", isAdmin = false)

        assertEquals(listOf("m3"), state.members.filter { it.isSelf }.map { it.id })
    }

    @Test
    fun `untagged includes members with null and empty tag ids`() {
        val state = adminState()

        assertEquals(setOf("me", "m3", "m4"), state.members.map { it.id }.toSet())
    }

    @Test
    fun `an admin with no tags puts everyone in members`() {
        val state = listing(
            members = listOf(member("me", "Me", GroupRole.Admin), member("m1", "Ann"))
        ).toSuccessState("me", isAdmin = true)

        assertTrue(state.tags.isEmpty())
        assertEquals(listOf("me", "m1"), state.members.map { it.id })
    }

    @Test
    fun `a member with an unknown tag id is in no section for an admin`() {
        val state = listing(
            members = listOf(member("m1", "Ann", tagId = "gone")),
            tags = listOf(MemberTag("t1", "A"))
        ).toSuccessState("x", isAdmin = true)

        assertTrue(state.members.isEmpty())
        assertTrue(state.tags.single().rows.isEmpty())
    }

    @Test
    fun `admin flags on another member`() {
        val other = adminState().members.first { it.id == "m3" }

        assertEquals(MemberStatus.Member, other.status)
        assertEquals("Dave", other.displayName)
        assertFalse(other.isSelf)
        assertTrue(other.canMoveToTag)
        assertTrue(other.canPromote)
        assertFalse(other.canDemote)
        assertTrue(other.canRemove)
        assertFalse(other.canRevokeInvitation)
        assertFalse(other.canResendInvitation)
        assertTrue(other.hasRoleActions)
        assertTrue(other.hasMenu)
    }

    @Test
    fun `admin flags on another admin`() {
        val other = adminState().tags.first { it.id == "t1" }.rows.first { it.id == "a1" }

        assertEquals(MemberStatus.Admin, other.status)
        assertFalse(other.canPromote)
        assertTrue(other.canDemote)
        assertTrue(other.canRemove)
        assertTrue(other.canMoveToTag)
    }

    @Test
    fun `an admin self row can only move to a tag`() {
        val self = adminState().members.first { it.isSelf }

        assertEquals("me", self.id)
        assertEquals("Zed", self.displayName)
        assertEquals(MemberStatus.Admin, self.status)
        assertFalse(self.canPromote)
        assertFalse(self.canDemote)
        assertFalse(self.canRemove)
        assertFalse(self.hasRoleActions)
        assertTrue(self.canMoveToTag)
        assertTrue(self.hasMenu)
    }

    @Test
    fun `pending rows have revoke and resend only`() {
        val pending = adminState().pendings.first { it.id == "p1" }

        assertEquals("zoe@example.com", pending.displayName)
        assertEquals(MemberStatus.Pending, pending.status)
        assertTrue(pending.canRevokeInvitation)
        assertTrue(pending.canResendInvitation)
        assertFalse(pending.canPromote)
        assertFalse(pending.canDemote)
        assertFalse(pending.canRemove)
        assertFalse(pending.canMoveToTag)
        assertFalse(pending.isSelf)
        assertNull(pending.tagId)
        assertTrue(pending.hasRoleActions)
        assertTrue(pending.hasMenu)
    }

    @Test
    fun `pendings are sorted case-insensitively`() {
        val state = listing(
            pendings = listOf(
                PendingInvitation("1", "charlie@x.com"),
                PendingInvitation("2", "Bob@x.com"),
                PendingInvitation("3", "alice@x.com")
            )
        ).toSuccessState("me", isAdmin = true)

        assertEquals(listOf("3", "2", "1"), state.pendings.map { it.id })
    }

    @Test
    fun `rows sort self first then admins then members case-insensitively`() {
        val state = listing(
            members = listOf(
                member("m1", "bravo"),
                member("a1", "Yankee", GroupRole.Admin),
                member("m2", "Alpha"),
                member("a2", "xray", GroupRole.Admin),
                member("me", "Zulu")
            )
        ).toSuccessState("me", isAdmin = true)

        assertEquals(listOf("me", "a2", "a1", "m2", "m1"), state.members.map { it.id })
    }

    @Test
    fun `rows are sorted inside a tag section`() {
        val state = adminState()

        assertEquals(listOf("a1", "m1"), state.tags.first { it.id == "t1" }.rows.map { it.id })
    }

    @Test
    fun `self comes first in a tag section`() {
        val state = listing(
            members = listOf(
                member("a1", "Anna", GroupRole.Admin, "t1"),
                member("me", "Zed", GroupRole.Member, "t1")
            ),
            tags = listOf(MemberTag("t1", "T"))
        ).toSuccessState("me", isAdmin = true)

        assertEquals(listOf("me", "a1"), state.tags.single().rows.map { it.id })
    }

    @Test
    fun `members are grouped under their tag`() {
        val state = adminState()

        assertEquals(listOf("a1", "m1"), state.tags[0].rows.map { it.id })
        assertEquals(listOf("m2"), state.tags[1].rows.map { it.id })
        assertTrue(state.tags[0].rows.all { it.tagId == "t1" })
        assertTrue(state.tags[1].rows.all { it.tagId == "t2" })
    }

    @Test
    fun `a tag without members has an empty section`() {
        val state = listing(tags = listOf(MemberTag("t1", "Empty"))).toSuccessState("me", true)

        assertTrue(state.tags.single().rows.isEmpty())
    }

    @Test
    fun `tag sections keep listing order with reorder flags`() {
        val state = listing(
            tags = listOf(MemberTag("c", "C"), MemberTag("a", "A"), MemberTag("b", "B"))
        ).toSuccessState("me", isAdmin = true)

        assertEquals(listOf("c", "a", "b"), state.tags.map { it.id })
        assertEquals(Reorder(canMoveUp = false, canMoveDown = true), state.tags[0].reorder)
        assertEquals(Reorder(canMoveUp = true, canMoveDown = true), state.tags[1].reorder)
        assertEquals(Reorder(canMoveUp = true, canMoveDown = false), state.tags[2].reorder)
    }

    @Test
    fun `a single tag cannot move either way`() {
        val state = listing(tags = listOf(MemberTag("t1", "Only"))).toSuccessState("me", true)

        assertEquals(Reorder(canMoveUp = false, canMoveDown = false), state.tags.single().reorder)
    }

    @Test
    fun `an empty listing gives an empty state`() {
        val state = listing().toSuccessState("me", isAdmin = true)

        assertTrue(state.tags.isEmpty())
        assertTrue(state.members.isEmpty())
        assertTrue(state.pendings.isEmpty())
        assertTrue(state.isEmpty)
    }

    @Test
    fun `sortRows orders self first then admins then members`() {
        val sorted = sortRows(
            listOf(
                row("1", "beta"),
                row("2", "Alpha", MemberStatus.Admin),
                row("3", "Self", MemberStatus.Member, isSelf = true),
                row("4", "alpha2"),
                row("5", "Zeta", MemberStatus.Admin)
            )
        )

        assertEquals(listOf("3", "2", "5", "4", "1"), sorted.map { it.id })
    }

    @Test
    fun `sortRows drops pending rows`() {
        val sorted = sortRows(listOf(row("1", "a@x.com", MemberStatus.Pending), row("2", "Bo")))

        assertEquals(listOf("2"), sorted.map { it.id })
    }

    @Test
    fun `sortRows of an empty list is empty`() {
        assertTrue(sortRows(emptyList()).isEmpty())
    }

    @Test
    fun `withMemberMoved moves a member from a tag to another tag`() {
        val state = adminState()
        val moving = state.tags[0].rows.first { it.id == "m1" }

        val result = state.withMemberMoved(moving, "t2")

        assertEquals(listOf("a1"), result.tags[0].rows.map { it.id })
        assertEquals(listOf("m2", "m1"), result.tags[1].rows.map { it.id })
        assertEquals("t2", result.tags[1].rows.first { it.id == "m1" }.tagId)
        assertEquals(state.members, result.members)
    }

    @Test
    fun `withMemberMoved moves a member from a tag to untagged`() {
        val state = adminState()
        val moving = state.tags[0].rows.first { it.id == "m1" }

        val result = state.withMemberMoved(moving, null)

        assertEquals(listOf("a1"), result.tags[0].rows.map { it.id })
        assertEquals(state.tags[1], result.tags[1])
        assertEquals(listOf("me", "m1", "m3", "m4"), result.members.map { it.id })
        assertNull(result.members.first { it.id == "m1" }.tagId)
    }

    @Test
    fun `withMemberMoved counts an empty tag id as untagged`() {
        val state = adminState()
        val moving = state.tags[1].rows.first()

        val result = state.withMemberMoved(moving, "")

        assertTrue(result.tags[1].rows.isEmpty())
        assertTrue(result.members.any { it.id == "m2" })
    }

    @Test
    fun `withMemberMoved moves a member from untagged to a tag`() {
        val state = adminState()
        val moving = state.members.first { it.id == "m3" }

        val result = state.withMemberMoved(moving, "t2")

        assertEquals(listOf("me", "m4"), result.members.map { it.id })
        assertEquals(listOf("m2", "m3"), result.tags[1].rows.map { it.id })
        assertEquals("t2", result.tags[1].rows.first { it.id == "m3" }.tagId)
        assertEquals(state.tags[0], result.tags[0])
    }

    @Test
    fun `withMemberMoved resorts the destination tag`() {
        val state = listing(
            members = listOf(
                member("m1", "Bea", tagId = "t1"),
                member("m2", "Dan", tagId = "t1"),
                member("m3", "Cat")
            ),
            tags = listOf(MemberTag("t1", "T"))
        ).toSuccessState("me", isAdmin = true)

        val result = state.withMemberMoved(state.members.single(), "t1")

        assertEquals(listOf("m1", "m3", "m2"), result.tags.single().rows.map { it.id })
    }

    @Test
    fun `withMemberMoved resorts untagged with admins before members`() {
        val state = listing(
            members = listOf(
                member("m1", "Ann"),
                member("a1", "Zed", GroupRole.Admin, "t1")
            ),
            tags = listOf(MemberTag("t1", "T"))
        ).toSuccessState("me", isAdmin = true)

        val result = state.withMemberMoved(state.tags.single().rows.single(), null)

        assertEquals(listOf("a1", "m1"), result.members.map { it.id })
    }

    @Test
    fun `withMemberMoved to the same tag keeps a single copy`() {
        val state = adminState()
        val moving = state.tags[0].rows.first { it.id == "m1" }

        val result = state.withMemberMoved(moving, "t1")

        assertEquals(1, result.tags.flatMap { it.rows }.count { it.id == "m1" })
        assertEquals(state.memberCount, result.memberCount)
    }

    @Test
    fun `withMemberMoved sets moving ids and clears errors`() {
        val state = adminState().copy(
            actionTargetId = "other",
            actionError = DomainError.Network(),
            failedActionId = "other",
            moveError = DomainError.Network()
        )
        val moving = state.members.first { it.id == "m3" }

        val result = state.withMemberMoved(moving, "t1")

        assertEquals("m3", result.movingProfileId)
        assertEquals("m3", result.actionTargetId)
        assertNull(result.actionError)
        assertNull(result.failedActionId)
        assertNull(result.moveError)
    }

    @Test
    fun `withMemberMoved keeps pendings and flags`() {
        val state = adminState().copy(isRefreshing = true)

        val result = state.withMemberMoved(state.members.first { it.id == "m3" }, "t1")

        assertEquals(state.pendings, result.pendings)
        assertTrue(result.isRefreshing)
        assertTrue(result.canAddMembers)
        assertTrue(result.canManageTags)
    }

    @Test
    fun `withMemberMoved preserves the member count`() {
        val state = adminState()

        val result = state.withMemberMoved(state.members.first { it.id == "m3" }, "t2")

        assertEquals(state.memberCount, result.memberCount)
        assertEquals(state.tags[1].rows.size + 1, result.tags[1].rows.size)
        assertEquals(state.members.size - 1, result.members.size)
    }
}
