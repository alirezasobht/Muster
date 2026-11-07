package app.muster.ui.navgraphs

import androidx.compose.material3.Text
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.navigation.compose.composable
import app.muster.ui.screens.event.Event
import app.muster.ui.screens.group.EditGroup
import app.muster.ui.screens.group.Group
import app.muster.ui.screens.group.events.NewEvent
import app.muster.ui.screens.group.groupGraph
import app.muster.ui.screens.group.members.AddMemberByEmail
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

// Event belongs to the event graph, so it is a stub here.
class GroupNavGraphTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val harness = NavGraphHarness(composeRule)

    private fun show() {
        harness.show(start = Group(id = "g1", name = "Sunday League")) { nav ->
            groupGraph(nav)
            composable<Event> { Text("event stub") }
        }
        harness.waitForText("Weekly 7s")
    }

    @Test
    fun editGroupOpensFromTheOverflowMenuWithTheGroupName() {
        show()
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Edit group").performClick()

        harness.assertAt<EditGroup>()
        assertEquals(EditGroup(groupId = "g1", groupName = "Sunday League"), harness.route<EditGroup>())
    }

    @Test
    fun backFromEditGroupReturnsToTheGroup() {
        show()
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Edit group").performClick()
        harness.assertAt<EditGroup>()

        composeRule.onNodeWithContentDescription("Back").performClick()

        harness.assertAt<Group>()
    }

    @Test
    fun savingANewNameReturnsToTheGroupAndLeavesEditGroupOffTheBackStack() {
        show()
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Edit group").performClick()
        harness.assertAt<EditGroup>()
        composeRule.onNode(hasSetTextAction()).performTextReplacement("Sunday League 2")
        composeRule.onNodeWithText("Save").performClick()

        harness.assertAt<Group>()
        assertTrue(!harness.onBackStack<EditGroup>())
    }

    @Test
    fun selectingAnEventOpensItWithTheCardsDetails() {
        show()
        composeRule.onNodeWithText("Weekly 7s").performClick()

        harness.assertAt<Event>()
        val route = harness.route<Event>()
        assertEquals("e1", route.eventId)
        assertEquals("g1", route.groupId)
        assertEquals("Sunday League", route.groupName)
        assertEquals("Weekly 7s", route.title)
        assertEquals(10, route.capacity)
        assertEquals("Westgate Pitch 2", route.location)
        assertEquals("Thu 1 Jan · 8:00 pm", route.date)
    }

    @Test
    fun newEventOpensForTheGroup() {
        show()
        composeRule.onNodeWithContentDescription("New event").performClick()

        harness.assertAt<NewEvent>()
        assertEquals(NewEvent(groupId = "g1", groupName = "Sunday League"), harness.route<NewEvent>())
    }

    @Test
    fun addByEmailOpensFromTheMembersTabAndCloseReturnsToTheGroup() {
        show()
        composeRule.onNodeWithText("Members").performClick()
        harness.waitForText("Add by email")
        composeRule.onNodeWithText("Add by email").performClick()

        harness.assertAt<AddMemberByEmail>()
        assertEquals(AddMemberByEmail(groupId = "g1", groupName = "Sunday League"), harness.route<AddMemberByEmail>())

        composeRule.onNodeWithContentDescription("Close").performClick()

        harness.assertAt<Group>()
        assertTrue(!harness.onBackStack<AddMemberByEmail>())
    }
}
