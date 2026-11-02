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
    fun admin_state_has_tags_untagged_pendings_and_flags() {
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
    fun non_admin_state_has_all_members_and_no_tags_or_pendings() {
        val state = fullListing.toSuccessState(myProfileId = "m3", isAdmin = false)

        assertTrue(state.tags.isEmpty())
        assertTrue(state.pendings.isEmpty())
        assertEquals(6, state.members.size)
        assertFalse(state.canAddMembers)
        assertFalse(state.canManageTags)
    }

    @Test
    fun non_admin_rows_have_no_action_flags() {
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
    fun non_admin_keeps_tag_ids_on_rows() {
        val state = fullListing.toSuccessState(myProfileId = "m3", isAdmin = false)

        assertEquals("t1", state.members.first { it.id == "a1" }.tagId)
    }

    @Test
    fun non_admin_self_row_is_marked_self() {
        val state = fullListing.toSuccessState(myProfileId = "m3", isAdmin = false)

        assertEquals(listOf("m3"), state.members.filter { it.isSelf }.map { it.id })
    }

    @Test
    fun untagged_includes_null_and_empty_tag_ids() {
        val state = adminState()

        assertEquals(setOf("me", "m3", "m4"), state.members.map { it.id }.toSet())
    }

    @Test
    fun admin_with_no_tags_puts_everyone_in_members() {
        val state = listing(
            members = listOf(member("me", "Me", GroupRole.Admin), member("m1", "Ann"))
        ).toSuccessState("me", isAdmin = true)

        assertTrue(state.tags.isEmpty())
        assertEquals(listOf("me", "m1"), state.members.map { it.id })
    }

    @Test
    fun member_with_unknown_tag_id_is_in_no_section_for_admin() {
        val state = listing(
            members = listOf(member("m1", "Ann", tagId = "gone")),
            tags = listOf(MemberTag("t1", "A"))
        ).toSuccessState("x", isAdmin = true)

        assertTrue(state.members.isEmpty())
        assertTrue(state.tags.single().rows.isEmpty())
    }

    @Test
    fun admin_flags_on_other_member() {
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
    fun admin_flags_on_other_admin() {
        val other = adminState().tags.first { it.id == "t1" }.rows.first { it.id == "a1" }

        assertEquals(MemberStatus.Admin, other.status)
        assertFalse(other.canPromote)
        assertTrue(other.canDemote)
        assertTrue(other.canRemove)
        assertTrue(other.canMoveToTag)
    }

    @Test
    fun admin_self_row_can_only_move_to_tag() {
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
    fun pending_rows_have_revoke_and_resend_only() {
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
    fun pendings_are_sorted_case_insensitively() {
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
    fun sorts_self_first_then_admins_then_members_case_insensitively() {
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
    fun sorts_rows_inside_tag_section() {
        val state = adminState()

        assertEquals(listOf("a1", "m1"), state.tags.first { it.id == "t1" }.rows.map { it.id })
    }

    @Test
    fun self_in_tag_section_comes_first() {
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
    fun members_are_grouped_under_their_tag() {
        val state = adminState()

        assertEquals(listOf("a1", "m1"), state.tags[0].rows.map { it.id })
        assertEquals(listOf("m2"), state.tags[1].rows.map { it.id })
        assertTrue(state.tags[0].rows.all { it.tagId == "t1" })
        assertTrue(state.tags[1].rows.all { it.tagId == "t2" })
    }

    @Test
    fun tag_without_members_has_empty_section() {
        val state = listing(tags = listOf(MemberTag("t1", "Empty"))).toSuccessState("me", true)

        assertTrue(state.tags.single().rows.isEmpty())
    }

    @Test
    fun tag_sections_keep_listing_order_with_reorder_flags() {
        val state = listing(
            tags = listOf(MemberTag("c", "C"), MemberTag("a", "A"), MemberTag("b", "B"))
        ).toSuccessState("me", isAdmin = true)

        assertEquals(listOf("c", "a", "b"), state.tags.map { it.id })
        assertEquals(Reorder(canMoveUp = false, canMoveDown = true), state.tags[0].reorder)
        assertEquals(Reorder(canMoveUp = true, canMoveDown = true), state.tags[1].reorder)
        assertEquals(Reorder(canMoveUp = true, canMoveDown = false), state.tags[2].reorder)
    }

    @Test
    fun single_tag_cannot_move_either_way() {
        val state = listing(tags = listOf(MemberTag("t1", "Only"))).toSuccessState("me", true)

        assertEquals(Reorder(canMoveUp = false, canMoveDown = false), state.tags.single().reorder)
    }

    @Test
    fun empty_listing_gives_empty_state() {
        val state = listing().toSuccessState("me", isAdmin = true)

        assertTrue(state.tags.isEmpty())
        assertTrue(state.members.isEmpty())
        assertTrue(state.pendings.isEmpty())
        assertTrue(state.isEmpty)
    }

    @Test
    fun sortRows_orders_self_admins_members() {
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
    fun sortRows_drops_pending_rows() {
        val sorted = sortRows(listOf(row("1", "a@x.com", MemberStatus.Pending), row("2", "Bo")))

        assertEquals(listOf("2"), sorted.map { it.id })
    }

    @Test
    fun sortRows_of_empty_list_is_empty() {
        assertTrue(sortRows(emptyList()).isEmpty())
    }

    @Test
    fun withMemberMoved_tag_to_other_tag() {
        val state = adminState()
        val moving = state.tags[0].rows.first { it.id == "m1" }

        val result = state.withMemberMoved(moving, "t2")

        assertEquals(listOf("a1"), result.tags[0].rows.map { it.id })
        assertEquals(listOf("m2", "m1"), result.tags[1].rows.map { it.id })
        assertEquals("t2", result.tags[1].rows.first { it.id == "m1" }.tagId)
        assertEquals(state.members, result.members)
    }

    @Test
    fun withMemberMoved_tag_to_untagged() {
        val state = adminState()
        val moving = state.tags[0].rows.first { it.id == "m1" }

        val result = state.withMemberMoved(moving, null)

        assertEquals(listOf("a1"), result.tags[0].rows.map { it.id })
        assertEquals(state.tags[1], result.tags[1])
        assertEquals(listOf("me", "m1", "m3", "m4"), result.members.map { it.id })
        assertNull(result.members.first { it.id == "m1" }.tagId)
    }

    @Test
    fun withMemberMoved_empty_tag_id_counts_as_untagged() {
        val state = adminState()
        val moving = state.tags[1].rows.first()

        val result = state.withMemberMoved(moving, "")

        assertTrue(result.tags[1].rows.isEmpty())
        assertTrue(result.members.any { it.id == "m2" })
    }

    @Test
    fun withMemberMoved_untagged_to_tag() {
        val state = adminState()
        val moving = state.members.first { it.id == "m3" }

        val result = state.withMemberMoved(moving, "t2")

        assertEquals(listOf("me", "m4"), result.members.map { it.id })
        assertEquals(listOf("m2", "m3"), result.tags[1].rows.map { it.id })
        assertEquals("t2", result.tags[1].rows.first { it.id == "m3" }.tagId)
        assertEquals(state.tags[0], result.tags[0])
    }

    @Test
    fun withMemberMoved_resorts_destination_tag() {
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
    fun withMemberMoved_resorts_untagged_with_admin_before_member() {
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
    fun withMemberMoved_to_same_tag_keeps_single_copy() {
        val state = adminState()
        val moving = state.tags[0].rows.first { it.id == "m1" }

        val result = state.withMemberMoved(moving, "t1")

        assertEquals(1, result.tags.flatMap { it.rows }.count { it.id == "m1" })
        assertEquals(state.memberCount, result.memberCount)
    }

    @Test
    fun withMemberMoved_sets_moving_ids_and_clears_errors() {
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
    fun withMemberMoved_keeps_pendings_and_flags() {
        val state = adminState().copy(isRefreshing = true)

        val result = state.withMemberMoved(state.members.first { it.id == "m3" }, "t1")

        assertEquals(state.pendings, result.pendings)
        assertTrue(result.isRefreshing)
        assertTrue(result.canAddMembers)
        assertTrue(result.canManageTags)
    }

    @Test
    fun withMemberMoved_preserves_member_count() {
        val state = adminState()

        val result = state.withMemberMoved(state.members.first { it.id == "m3" }, "t2")

        assertEquals(state.memberCount, result.memberCount)
        assertEquals(state.tags[1].rows.size + 1, result.tags[1].rows.size)
        assertEquals(state.members.size - 1, result.members.size)
    }
}
