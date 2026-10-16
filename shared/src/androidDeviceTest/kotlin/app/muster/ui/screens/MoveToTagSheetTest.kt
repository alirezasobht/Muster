package app.muster.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.muster.ui.screens.group.members.MoveToTagActions
import app.muster.ui.screens.group.members.MoveToTagSheet
import app.muster.ui.screens.group.members.TagOption
import app.muster.ui.theme.MusterTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MoveToTagSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val tags = listOf(TagOption("t1", "Regulars", 6), TagOption("t2", "Casual", 4))

    private fun show(
        currentTagId: String? = null,
        onPick: (String?) -> Unit = {},
        onNewTag: () -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        composeRule.setContent {
            MusterTheme {
                MoveToTagSheet(
                    memberName = "Sam Okafor",
                    currentTagId = currentTagId,
                    tags = tags,
                    actions = MoveToTagActions(onPick = onPick, onNewTag = onNewTag, onDismiss = onDismiss)
                )
            }
        }
    }

    @Test
    fun showsTitleTagsWithCountsAndActions() {
        show(currentTagId = null)
        composeRule.onNodeWithText("Move Sam Okafor to").assertIsDisplayed()
        composeRule.onNodeWithText("Regulars").assertIsDisplayed()
        composeRule.onNodeWithText("6").assertIsDisplayed()
        composeRule.onNodeWithText("Casual").assertIsDisplayed()
        composeRule.onNodeWithText("No tag").assertIsDisplayed()
        composeRule.onNodeWithText("New tag…").assertIsDisplayed()
    }

    @Test
    fun theCurrentTagIsSelected() {
        show(currentTagId = "t2")
        composeRule.onNodeWithText("Casual").assertIsSelected()
    }

    @Test
    fun noTagIsSelectedWhenUntagged() {
        show(currentTagId = null)
        composeRule.onNodeWithText("No tag").assertIsSelected()
    }

    @Test
    fun pickingATagReachesTheCallbackWithItsId() {
        var picked: String? = "unset"
        show(currentTagId = null, onPick = { picked = it })
        composeRule.onNodeWithText("Regulars").performClick()
        assertEquals("t1", picked)
    }

    @Test
    fun noTagReachesTheCallbackWithNull() {
        var picked: String? = "unset"
        show(currentTagId = "t1", onPick = { picked = it })
        composeRule.onNodeWithText("No tag").performClick()
        assertEquals(null, picked)
    }

    @Test
    fun pickingTheCurrentTagJustCloses() {
        var picked = false
        var dismissed = false
        show(currentTagId = "t1", onPick = { picked = true }, onDismiss = { dismissed = true })
        composeRule.onNodeWithText("Regulars").performClick()
        assertEquals(false, picked)
        assertEquals(true, dismissed)
    }

    @Test
    fun newTagReachesTheCallback() {
        var clicked = false
        show(currentTagId = null, onNewTag = { clicked = true })
        composeRule.onNodeWithText("New tag…").performClick()
        assertEquals(true, clicked)
    }
}
