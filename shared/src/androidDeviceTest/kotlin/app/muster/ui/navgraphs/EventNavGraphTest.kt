package app.muster.ui.navgraphs

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import app.muster.ui.screens.event.AddPlayers
import app.muster.ui.screens.event.EditEvent
import app.muster.ui.screens.event.Event
import app.muster.ui.screens.event.eventGraph
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class EventNavGraphTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val harness = NavGraphHarness(composeRule)

    private fun show() {
        harness.show(
            start = Event(
                groupId = "g1",
                groupName = "Sunday League",
                eventId = "e1",
                title = "Weekly 7s",
                date = "Thu 1 Jan",
                location = "Westgate Pitch 2",
                capacity = 10,
                inCount = 0,
                pendingCount = 0
            )
        ) { nav -> eventGraph(nav) }
        harness.waitForText("Weekly 7s")
    }

    @Test
    fun addPlayersOpensForTheEventAndBackReturnsToIt() {
        show()
        harness.waitForText("+ Add players")
        composeRule.onNodeWithText("+ Add players").performClick()

        harness.assertAt<AddPlayers>()
        assertEquals(AddPlayers(eventId = "e1", groupId = "g1"), harness.route<AddPlayers>())

        composeRule.onNodeWithContentDescription("Back").performClick()

        harness.assertAt<Event>()
    }

    @Test
    fun editEventOpensFromTheOverflowMenuWithTheEventsValues() {
        show()
        harness.waitForText("+ Add players")
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Edit event").performClick()

        harness.assertAt<EditEvent>()
        val route = harness.route<EditEvent>()
        assertEquals("e1", route.eventId)
        assertEquals("g1", route.groupId)
        assertEquals("Weekly 7s", route.title)
        assertEquals("Westgate Pitch 2", route.location)
    }

    @Test
    fun savingAnEditedEventReturnsToTheEventAndLeavesEditEventOffTheBackStack() {
        show()
        harness.waitForText("+ Add players")
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Edit event").performClick()
        harness.assertAt<EditEvent>()
        composeRule.onNodeWithText("Weekly 7s").performTextReplacement("Friday 7s")
        composeRule.onNodeWithText("Save").performClick()

        harness.assertAt<Event>()
        assertTrue(!harness.onBackStack<EditEvent>())
    }

    @Test
    fun backFromEditEventReturnsToTheEvent() {
        show()
        harness.waitForText("+ Add players")
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Edit event").performClick()
        harness.assertAt<EditEvent>()

        composeRule.onNodeWithContentDescription("Back").performClick()

        harness.assertAt<Event>()
    }
}
