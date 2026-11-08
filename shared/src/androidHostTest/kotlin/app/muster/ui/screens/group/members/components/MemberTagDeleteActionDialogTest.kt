package app.muster.ui.screens.group.members.components

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.muster.ui.screens.group.members.MemberRow
import app.muster.ui.screens.group.members.MemberStatus
import app.muster.ui.screens.group.members.Reorder
import app.muster.ui.screens.group.members.TagSection
import app.muster.ui.theme.MusterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MemberTagDeleteActionDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun tag(memberCount: Int) = TagSection(
        id = "t1",
        name = "Regular",
        rows = (1..memberCount).map {
            MemberRow(id = it.toString(), displayName = "Member $it", status = MemberStatus.Member)
        },
        reorder = Reorder(canMoveUp = false, canMoveDown = false)
    )

    private fun show(
        section: TagSection,
        onDeleteTag: (String) -> Unit = {}
    ) {
        composeRule.setContent {
            MusterTheme {
                val request = rememberTagDeleteAction(onDeleteTag)
                Button(onClick = { request(section) }) { Text("Open") }
            }
        }
    }

    @Test
    fun nothingIsShownUntilRequested() {
        show(tag(2))
        composeRule.onNodeWithText("Delete “Regular”?").assertDoesNotExist()
    }

    @Test
    fun theDialogNamesTheTagAndCountsASingleMember() {
        show(tag(1))
        composeRule.onNodeWithText("Open").performClick()
        composeRule.onNodeWithText("Delete “Regular”?").assertIsDisplayed()
        composeRule.onNodeWithText("Its 1 member moves", substring = true).assertIsDisplayed()
    }

    @Test
    fun theDialogCountsSeveralMembers() {
        show(tag(3))
        composeRule.onNodeWithText("Open").performClick()
        composeRule.onNodeWithText("Its 3 members move", substring = true).assertIsDisplayed()
    }

    @Test
    fun theDialogSaysSoForAnEmptyTag() {
        show(tag(0))
        composeRule.onNodeWithText("Open").performClick()
        composeRule.onNodeWithText("No one has this tag.").assertIsDisplayed()
    }

    @Test
    fun confirmingCallsBackWithTheTagIdAndClosesTheDialog() {
        var deleted: String? = null
        show(tag(2), onDeleteTag = { deleted = it })
        composeRule.onNodeWithText("Open").performClick()
        assert(deleted == null)
        composeRule.onNodeWithText("Delete").performClick()
        assert(deleted == "t1")
        composeRule.onNodeWithText("Delete “Regular”?").assertDoesNotExist()
    }

    @Test
    fun cancellingClosesTheDialogWithoutCallingBack() {
        var deleted: String? = null
        show(tag(2), onDeleteTag = { deleted = it })
        composeRule.onNodeWithText("Open").performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        assert(deleted == null)
        composeRule.onNodeWithText("Delete “Regular”?").assertDoesNotExist()
    }
}
