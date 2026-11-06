package app.muster.ui.screens.event.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.muster.ui.screens.event.EventPhase
import app.muster.ui.theme.MusterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EventPhaseStripTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(phase: EventPhase) {
        composeRule.setContent {
            MusterTheme {
                EventPhaseStrip(phase = phase)
            }
        }
    }

    @Test
    fun aStartedEventShowsTheStartedLine() {
        show(EventPhase.Started(atDisplay = "7:00 pm"))
        composeRule.onNodeWithText("Started at 7:00 pm · no more changes").assertIsDisplayed()
    }

    @Test
    fun aLockedEventShowsBothStripLines() {
        show(EventPhase.Locked(byName = "Ali"))
        composeRule.onNodeWithText("Locked by Ali").assertIsDisplayed()
        composeRule.onNodeWithText("No changes until an admin unlocks it").assertIsDisplayed()
    }

    @Test
    fun aLockedEventWithNoLockerNameFallsBackToAnAdmin() {
        show(EventPhase.Locked(byName = null))
        composeRule.onNodeWithText("Locked by an admin").assertIsDisplayed()
        composeRule.onNodeWithText("No changes until an admin unlocks it").assertIsDisplayed()
    }

    @Test
    fun anActiveEventShowsNoStrip() {
        show(EventPhase.Active)
        composeRule.onNodeWithText("No changes until an admin unlocks it").assertDoesNotExist()
        composeRule.onNodeWithText("no more changes", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("This event won’t go ahead").assertDoesNotExist()
    }

    @Test
    fun aCancelledEventShowsBothStripLines() {
        show(EventPhase.Cancelled(byName = "Ali", started = false))
        composeRule.onNodeWithText("Cancelled by Ali").assertIsDisplayed()
        composeRule.onNodeWithText("This event won’t go ahead").assertIsDisplayed()
    }

    @Test
    fun aCancelledEventWithNoCancellerNameFallsBackToAnAdmin() {
        show(EventPhase.Cancelled(byName = null, started = false))
        composeRule.onNodeWithText("Cancelled by an admin").assertIsDisplayed()
    }
}
