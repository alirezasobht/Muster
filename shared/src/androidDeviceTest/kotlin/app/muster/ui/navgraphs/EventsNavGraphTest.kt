package app.muster.ui.navgraphs

import androidx.compose.material3.Text
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.composable
import app.muster.ui.screens.event.Event
import app.muster.ui.screens.group.events.NewEvent
import app.muster.ui.screens.group.events.eventsGraph
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.Serializable
import org.junit.Rule
import org.junit.Test

// Event belongs to the event graph, so it is a stub here.
class EventsNavGraphTest {

    @Serializable
    private data object Start

    @get:Rule
    val composeRule = createComposeRule()

    private val harness = NavGraphHarness(composeRule)

    private fun showNewEvent() {
        harness.show(start = Start) { nav ->
            composable<Start> { Text("start stub") }
            composable<Event> { Text("event stub") }
            eventsGraph(nav)
        }
        composeRule.runOnIdle { harness.navController.navigate(NewEvent(groupId = "g1", groupName = "Sunday League")) }
        harness.assertAt<NewEvent>()
        harness.waitForText("Create event")
    }

    // The picked day is in next month so it is always in the future; the time is whatever the picker
    // opens on.
    private fun pickDateAndTime() {
        composeRule.onNodeWithText("Select date").performClick()
        composeRule.onNodeWithContentDescription("Change to next month", substring = true).performClick()
        // Day cells read like "Sunday, November 15, 2026". The neighbouring month page stays composed,
        // so the visible month is the first match.
        composeRule.onAllNodes(hasText(" 15, ", substring = true) and hasClickAction())[0].performClick()
        composeRule.onNodeWithText("OK").performClick()
        composeRule.onNodeWithText("Select time").performClick()
        composeRule.onNodeWithText("OK").performClick()
    }

    @Test
    fun backFromNewEventReturnsToWhereItWasOpened() {
        showNewEvent()

        composeRule.onNodeWithContentDescription("Back").performClick()

        harness.assertAt<Start>()
    }

    @Test
    fun creatingAnEventOpensItAndLeavesNewEventOffTheBackStack() {
        showNewEvent()
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("Friday 7s")
        pickDateAndTime()
        composeRule.onNodeWithText("Create event").performClick()

        harness.assertAt<Event>()
        val route = harness.route<Event>()
        assertEquals("Friday 7s", route.title)
        assertEquals("g1", route.groupId)
        assertEquals("Sunday League", route.groupName)
        assertEquals(0, route.inCount)
        assertTrue(!harness.onBackStack<NewEvent>())
    }
}
