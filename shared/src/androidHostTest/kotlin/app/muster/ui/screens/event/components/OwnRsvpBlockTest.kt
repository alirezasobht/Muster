package app.muster.ui.screens.event.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.muster.domain.error.DomainError
import app.muster.domain.model.RsvpStatus
import app.muster.ui.screens.event.EventPhase
import app.muster.ui.screens.event.EventUiState
import app.muster.ui.screens.event.TestLocked
import app.muster.ui.screens.event.TestSuccess
import app.muster.ui.theme.MusterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OwnRsvpBlockTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        state: EventUiState.Success,
        onRsvp: (RsvpStatus) -> Unit = {}
    ) {
        composeRule.setContent {
            MusterTheme {
                LazyColumn {
                    ownRsvpSection(state = state, onRsvp = onRsvp)
                }
            }
        }
    }

    @Test
    fun unansweredShowsTheQuestionAndTwoButtons() {
        show(state = TestSuccess.copy(myStatus = RsvpStatus.Pending))
        composeRule.onNodeWithText("Can you play?").assertIsDisplayed()
        composeRule.onNodeWithText("I’m in").assertIsDisplayed()
        composeRule.onNodeWithText("Can’t make it").assertIsDisplayed()
    }

    @Test
    fun tappingImInReachesOnRsvpWithIn() {
        var status: RsvpStatus? = null
        show(state = TestSuccess.copy(myStatus = RsvpStatus.Pending), onRsvp = { status = it })
        composeRule.onNodeWithText("I’m in").performClick()
        assert(status == RsvpStatus.In)
    }

    @Test
    fun tappingCantMakeItReachesOnRsvpWithOut() {
        var status: RsvpStatus? = null
        show(state = TestSuccess.copy(myStatus = RsvpStatus.Pending), onRsvp = { status = it })
        composeRule.onNodeWithText("Can’t make it").performClick()
        assert(status == RsvpStatus.Out)
    }

    @Test
    fun answeredInShowsTheCollapsedBlockWithChange() {
        show(state = TestSuccess.copy(myStatus = RsvpStatus.In))
        composeRule.onNodeWithText("You’re in").assertIsDisplayed()
        composeRule.onNodeWithText("Change").assertIsDisplayed()
    }

    @Test
    fun answeredOutShowsYoureOut() {
        show(state = TestSuccess.copy(myStatus = RsvpStatus.Out))
        composeRule.onNodeWithText("You’re out").assertIsDisplayed()
    }

    @Test
    fun tappingChangeReopensTheTwoButtonsWithoutACallback() {
        var rsvpCalled = false
        show(state = TestSuccess.copy(myStatus = RsvpStatus.In), onRsvp = { rsvpCalled = true })
        composeRule.onNodeWithText("Change").performClick()
        composeRule.onNodeWithText("Can you play?").assertIsDisplayed()
        assert(!rsvpCalled)
    }

    @Test
    fun aRsvpErrorIsShownInTheBlock() {
        show(state = TestSuccess.copy(myStatus = RsvpStatus.In, rsvpError = DomainError.EventFull()))
        composeRule.onNodeWithText("This event is full.").assertIsDisplayed()
    }

    @Test
    fun noRsvpBlockWhenTheViewerWasNeverInvited() {
        show(state = TestSuccess.copy(myStatus = null))
        composeRule.onNodeWithText("Can you play?").assertDoesNotExist()
        composeRule.onNodeWithText("You’re in").assertDoesNotExist()
    }

    @Test
    fun frozenRsvpBlockIsStaticWithNoChangeButton() {
        show(state = TestSuccess.copy(phase = EventPhase.Started(atDisplay = "7:00 pm"), myStatus = RsvpStatus.In))
        composeRule.onNodeWithText("You played").assertIsDisplayed()
        composeRule.onNodeWithText("Change").assertDoesNotExist()
    }

    @Test
    fun lockedRsvpBlockIsStaticWithNoChangeButton() {
        show(state = TestLocked.copy(myStatus = RsvpStatus.In))
        composeRule.onNodeWithText("Your reply").assertIsDisplayed()
        composeRule.onNodeWithText("Change").assertDoesNotExist()
        composeRule.onNodeWithText("You played").assertDoesNotExist()
    }

    @Test
    fun lockedUnansweredRsvpOffersNoButtons() {
        show(state = TestLocked.copy(myStatus = RsvpStatus.Pending))
        composeRule.onNodeWithText("Can you play?").assertDoesNotExist()
        composeRule.onNodeWithText("I’m in").assertDoesNotExist()
    }
}
