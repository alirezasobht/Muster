package app.muster.ui.screens.group.members

import app.muster.ui.screens.group.members.movetotag.TagOption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MembersTabUiStateTest {

    private fun row(
        id: String,
        isSelf: Boolean = false,
        status: MemberStatus = MemberStatus.Member
    ) = MemberRow(id = id, displayName = "Name $id", status = status, isSelf = isSelf)

    private fun section(
        id: String,
        vararg rows: MemberRow
    ) = TagSection(
        id = id,
        name = "Tag $id",
        rows = rows.toList(),
        reorder = Reorder(canMoveUp = false, canMoveDown = false)
    )

    @Test
    fun `counts cover tags and untagged but not pendings`() {
        val state = MembersTabUiState.Success(
            tags = listOf(section("t1", row("1"), row("2")), section("t2", row("3"))),
            members = listOf(row("4")),
            pendings = listOf(row("p1", status = MemberStatus.Pending))
        )

        assertEquals(4, state.memberCount)
        assertEquals(1, state.pendingCount)
    }

    @Test
    fun `the default state is empty with zero counts`() {
        val state = MembersTabUiState.Success()

        assertTrue(state.isEmpty)
        assertEquals(0, state.memberCount)
        assertEquals(0, state.pendingCount)
    }

    @Test
    fun `a state with only self is empty`() {
        val state = MembersTabUiState.Success(members = listOf(row("1", isSelf = true)))

        assertTrue(state.isEmpty)
    }

    @Test
    fun `another member makes the state non-empty`() {
        val state = MembersTabUiState.Success(members = listOf(row("1", isSelf = true), row("2")))

        assertFalse(state.isEmpty)
    }

    @Test
    fun `a member in a tag makes the state non-empty`() {
        val state = MembersTabUiState.Success(tags = listOf(section("t1", row("2"))))

        assertFalse(state.isEmpty)
    }

    @Test
    fun `a pending makes the state non-empty`() {
        val state = MembersTabUiState.Success(
            members = listOf(row("1", isSelf = true)),
            pendings = listOf(row("p1", status = MemberStatus.Pending))
        )

        assertFalse(state.isEmpty)
    }

    @Test
    fun `the tags intro shows for tag managers without tags`() {
        val state = MembersTabUiState.Success(canManageTags = true)

        assertTrue(state.showTagsIntro)
        assertFalse(state.showNewTagButton)
    }

    @Test
    fun `the new tag button shows for tag managers with tags`() {
        val state = MembersTabUiState.Success(tags = listOf(section("t1")), canManageTags = true)

        assertFalse(state.showTagsIntro)
        assertTrue(state.showNewTagButton)
    }

    @Test
    fun `the tag intro and button are hidden when tags cannot be managed`() {
        assertFalse(MembersTabUiState.Success().showTagsIntro)
        assertFalse(MembersTabUiState.Success(tags = listOf(section("t1"))).showNewTagButton)
    }

    @Test
    fun `tagOptions map id name and member count`() {
        val state = MembersTabUiState.Success(
            tags = listOf(section("t1", row("1"), row("2")), section("t2"))
        )

        assertEquals(
            listOf(TagOption("t1", "Tag t1", 2), TagOption("t2", "Tag t2", 0)),
            state.tagOptions
        )
    }

    @Test
    fun `menu flags default to none`() {
        val plain = row("1")

        assertFalse(plain.hasRoleActions)
        assertFalse(plain.hasMenu)
    }

    @Test
    fun `each role action flag gives role actions and a menu`() {
        listOf(
            row("1").copy(canPromote = true),
            row("1").copy(canDemote = true),
            row("1").copy(canRemove = true),
            row("1").copy(canRevokeInvitation = true),
            row("1").copy(canResendInvitation = true)
        ).forEach {
            assertTrue(it.hasRoleActions)
            assertTrue(it.hasMenu)
        }
    }

    @Test
    fun `move to tag alone gives a menu without role actions`() {
        val movable = row("1").copy(canMoveToTag = true)

        assertFalse(movable.hasRoleActions)
        assertTrue(movable.hasMenu)
    }

    @Test
    fun `inFlight matches only the target id`() {
        val target = row("1")

        assertTrue(target.inFlight("1"))
        assertFalse(target.inFlight("2"))
        assertFalse(target.inFlight())
        assertFalse(target.inFlight(null))
    }
}
