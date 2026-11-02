package app.muster.ui.screens.group.members.tagcard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.muster.ui.screens.group.members.MemberRow
import app.muster.ui.screens.group.members.MemberStatus
import app.muster.ui.screens.group.members.Reorder
import app.muster.ui.screens.group.members.TagSection
import app.muster.ui.screens.group.members.components.MemberRowActions
import app.muster.ui.screens.group.members.components.MemberRowItem
import app.muster.ui.screens.group.members.components.MemberTagCard
import app.muster.ui.screens.group.members.components.MemberTagCardActions
import app.muster.ui.screens.group.members.components.TagCardRowPadding
import app.muster.ui.theme.MusterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MemberTagCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sam = MemberRow(
        id = "4",
        displayName = "Sam Okafor",
        status = MemberStatus.Member,
        canMoveToTag = true,
        canPromote = true,
        canRemove = true
    )

    private val regular = TagSection(
        id = "t1",
        name = "Regular",
        rows = listOf(sam),
        reorder = Reorder(canMoveUp = false, canMoveDown = true)
    )

    private fun show(
        section: TagSection,
        actions: MemberTagCardActions = MemberTagCardActions(),
        rowActions: MemberRowActions = MemberRowActions()
    ) {
        composeRule.setContent {
            MusterTheme {
                MemberTagCard(section = section, actions = actions) {
                    MemberRowItem(
                        row = it,
                        memberRowActions = rowActions,
                        inFlight = false,
                        errorMessage = null,
                        contentPadding = TagCardRowPadding
                    )
                }
            }
        }
    }

    @Test
    fun theHeaderShowsTheNameAndCount() {
        show(regular)
        composeRule.onNodeWithText("Regular").assertIsDisplayed()
        composeRule.onNodeWithText("1").assertIsDisplayed()
        composeRule.onNodeWithText("Sam Okafor").assertIsDisplayed()
    }

    @Test
    fun anEmptyTagShowsTheHint() {
        show(regular.copy(rows = emptyList()))
        composeRule.onNode(hasText("No one yet", substring = true)).assertIsDisplayed()
    }

    // 5l: Move up/down are greyed out, not hidden, at the ends.
    @Test
    fun theCardMenuGreysOutMovesPastTheEnds() {
        show(regular)
        composeRule.onNodeWithContentDescription("Tag options").performClick()
        composeRule.onNodeWithText("Rename").assertIsEnabled()
        composeRule.onNodeWithText("Move up").assertIsNotEnabled()
        composeRule.onNodeWithText("Move down").assertIsEnabled()
        composeRule.onNodeWithText("Delete tag").assertIsEnabled()
    }

    @Test
    fun theCardMenuReachesItsCallbacks() {
        var picked = ""
        show(
            regular,
            actions = MemberTagCardActions(
                onRename = { picked += "rename," },
                onMoveDown = { picked += "down," },
                onDelete = { picked += "delete," }
            )
        )
        listOf("Rename", "Move down", "Delete tag").forEach {
            composeRule.onNodeWithContentDescription("Tag options").performClick()
            composeRule.onNodeWithText(it).performClick()
        }
        assert(picked == "rename,down,delete,")
    }

    // 5j: Move to tag first, then the role items.
    @Test
    fun aTaggedRowMenuListsMoveToTagThenRoleItems() {
        var moved = false
        show(regular, rowActions = MemberRowActions(onMoveToTag = { moved = true }))
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Make admin").assertIsDisplayed()
        composeRule.onNodeWithText("Move to tag…").performClick()
        assert(moved)
    }

    // Your own row has no role items, but still gets ⋮ for Move to tag.
    @Test
    fun yourOwnRowGetsAMenuWithMoveToTagOnly() {
        show(regular.copy(rows = listOf(sam.copy(isSelf = true, canPromote = false, canRemove = false))))
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Move to tag…").assertIsDisplayed()
        composeRule.onNodeWithText("Make admin").assertDoesNotExist()
    }
}
