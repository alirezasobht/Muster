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

    private fun section(id: String, vararg rows: MemberRow) = TagSection(
        id = id,
        name = "Tag $id",
        rows = rows.toList(),
        reorder = Reorder(canMoveUp = false, canMoveDown = false)
    )

    @Test
    fun counts_cover_tags_and_untagged_but_not_pendings() {
        val state = MembersTabUiState.Success(
            tags = listOf(section("t1", row("1"), row("2")), section("t2", row("3"))),
            members = listOf(row("4")),
            pendings = listOf(row("p1", status = MemberStatus.Pending))
        )

        assertEquals(4, state.memberCount)
        assertEquals(1, state.pendingCount)
    }

    @Test
    fun default_state_is_empty_with_zero_counts() {
        val state = MembersTabUiState.Success()

        assertTrue(state.isEmpty)
        assertEquals(0, state.memberCount)
        assertEquals(0, state.pendingCount)
    }

    @Test
    fun only_self_is_empty() {
        val state = MembersTabUiState.Success(members = listOf(row("1", isSelf = true)))

        assertTrue(state.isEmpty)
    }

    @Test
    fun other_member_makes_state_non_empty() {
        val state = MembersTabUiState.Success(members = listOf(row("1", isSelf = true), row("2")))

        assertFalse(state.isEmpty)
    }

    @Test
    fun member_in_tag_makes_state_non_empty() {
        val state = MembersTabUiState.Success(tags = listOf(section("t1", row("2"))))

        assertFalse(state.isEmpty)
    }

    @Test
    fun pending_makes_state_non_empty() {
        val state = MembersTabUiState.Success(
            members = listOf(row("1", isSelf = true)),
            pendings = listOf(row("p1", status = MemberStatus.Pending))
        )

        assertFalse(state.isEmpty)
    }

    @Test
    fun tags_intro_shows_for_tag_managers_without_tags() {
        val state = MembersTabUiState.Success(canManageTags = true)

        assertTrue(state.showTagsIntro)
        assertFalse(state.showNewTagButton)
    }

    @Test
    fun new_tag_button_shows_for_tag_managers_with_tags() {
        val state = MembersTabUiState.Success(tags = listOf(section("t1")), canManageTags = true)

        assertFalse(state.showTagsIntro)
        assertTrue(state.showNewTagButton)
    }

    @Test
    fun tag_intro_and_button_hidden_when_cannot_manage_tags() {
        assertFalse(MembersTabUiState.Success().showTagsIntro)
        assertFalse(MembersTabUiState.Success(tags = listOf(section("t1"))).showNewTagButton)
    }

    @Test
    fun tagOptions_map_id_name_and_member_count() {
        val state = MembersTabUiState.Success(
            tags = listOf(section("t1", row("1"), row("2")), section("t2"))
        )

        assertEquals(
            listOf(TagOption("t1", "Tag t1", 2), TagOption("t2", "Tag t2", 0)),
            state.tagOptions
        )
    }

    @Test
    fun menu_flags_default_to_none() {
        val plain = row("1")

        assertFalse(plain.hasRoleActions)
        assertFalse(plain.hasMenu)
    }

    @Test
    fun each_role_action_flag_gives_role_actions_and_menu() {
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
    fun move_to_tag_alone_gives_menu_without_role_actions() {
        val movable = row("1").copy(canMoveToTag = true)

        assertFalse(movable.hasRoleActions)
        assertTrue(movable.hasMenu)
    }

    @Test
    fun inFlight_matches_only_the_target_id() {
        val target = row("1")

        assertTrue(target.inFlight("1"))
        assertFalse(target.inFlight("2"))
        assertFalse(target.inFlight())
        assertFalse(target.inFlight(null))
    }
}
