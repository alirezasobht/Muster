package app.muster.ui.screens.event.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import app.muster.ui.screens.event.EventPhase
import app.muster.ui.screens.event.EventUiState
import app.muster.ui.screens.event.TestLocked
import app.muster.ui.screens.event.TestSuccess
import app.muster.ui.screens.event.TestSummary
import app.muster.ui.theme.MusterTheme
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
class EventDetailsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        state: EventUiState.Success,
        onLockToggled: (Boolean) -> Unit = {}
    ) {
        composeRule.setContent {
            MusterTheme {
                LazyColumn {
                    detailsSection(state = state, onLockToggled = onLockToggled)
                }
            }
        }
    }

    @Test
    fun frozenSlotsLineReadsNoReplyInsteadOfPending() {
        show(state = TestSuccess.copy(phase = EventPhase.Started(atDisplay = "7:00 pm")))
        composeRule.onNodeWithText("of 10 slots").assertIsDisplayed()
        composeRule.onNodeWithText("6 open").assertIsDisplayed()
        composeRule.onNodeWithText("3 in").assertIsDisplayed()
        composeRule.onNodeWithText("1 no reply").assertIsDisplayed()
        composeRule.onNodeWithText("1 out").assertIsDisplayed()
        composeRule.onNodeWithText("1 pending").assertDoesNotExist()
    }

    @Test
    fun theLockSwitchIsShownWhenTheViewerCanToggleIt() {
        show(state = TestSuccess.copy(lockFeatureFlag = true))
        composeRule.onNodeWithText("Locked").assertIsDisplayed()
        composeRule.onNode(isToggleable()).assertIsOff()
    }

    @Test
    fun noLockSwitchWhenTheViewerCannotToggleIt() {
        show(state = TestSuccess.copy(lockFeatureFlag = false))
        composeRule.onNode(isToggleable()).assertDoesNotExist()
        composeRule.onNodeWithText("Locked").assertDoesNotExist()
    }

    @Test
    fun theLockSwitchReflectsALockedEvent() {
        show(state = TestLocked)
        composeRule.onNode(isToggleable()).assertIsOn()
    }

    @Test
    fun tappingTheLockSwitchReportsTheRequestedValue() {
        var toggled: Boolean? = null
        show(state = TestSuccess.copy(lockFeatureFlag = true), onLockToggled = { toggled = it })
        composeRule.onNode(isToggleable()).performClick()
        assert(toggled == true)
    }

    // LEGACY graphics doesn't measure text, so every string fits on one line.
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test
    fun longDetailsBesideTheLockSwitchWrapInsteadOfTruncating() {
        val date = "Wednesday 17 September 2026 · 7:00 pm until 9:30 pm, kick-off sharp"
        val location = "Westgate Community Sports Centre, Pitch 2 behind the car park, Long Lane, Westgate"
        show(state = TestSuccess.copy(lockFeatureFlag = true, summary = TestSummary.copy(date = date, location = location)))
        composeRule.onNode(isToggleable()).assertIsDisplayed()
        for (text in listOf(date, location)) {
            val node = composeRule.onNodeWithText(text).assertIsDisplayed()
            val layout = node.textLayout()
            assertTrue(layout.lineCount > 1, "\"$text\" took ${layout.lineCount} line(s)")
            assertFalse(layout.hasVisualOverflow, "\"$text\" overflows")
        }
    }

    private fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.single()
    }
}
